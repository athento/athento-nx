package org.athento.nx.upgrade.stats;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.api.GenericInfo;
import org.athento.nx.upgrade.util.UpgradeUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.client.node.NodeClient;
import org.nuxeo.ecm.automation.core.util.RecordSet;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.io.DocumentsExporter;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.ElasticSearchComponent;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import javax.swing.text.Document;
import java.io.Serializable;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation for Athento Stats service.
 */
public class AthentoStatsServiceImpl extends DefaultComponent implements AthentoStatsService {

    private static final Log LOG = LogFactory.getLog(AthentoStatsServiceImpl.class);


    @Override
    public void startCalculation(String repositoryName, String [] doctypes, String path) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting calculations...");
        }
        WorkManager workManager = Framework.getService(WorkManager.class);
        if (workManager == null) {
            throw new RuntimeException("No WorkManager available");
        }
        Work work = new AthentoStatsInitialWork(repositoryName, doctypes, path);
        workManager.schedule(work);
    }

    @Override
    public void startCalculation(CoreSession session, String stat, String statName, String path) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting calculation for " + stat + " " + statName + " " + path + "...");
        }
        RegisterStatInfo registerStatInfo = RegisterStatInfo.getInstance();
        StatInfo mainStatInfo = registerStatInfo.getStat(stat);
        if (DocumentStats.STAT.equals(stat)) {
            String doctype = getDoctypeForStat(statName);
            LOG.info("Refreshing " + statName + " for doctype " + doctype);
            DocumentStats docs = new DocumentStats(path);
            if (doctype != null) {
                docs.calculateForDoctype(session, mainStatInfo, doctype, statName);
            } else {
                docs.calculate(session, mainStatInfo, statName);
            }
        } else if (DiskStats.STAT.equals(stat)) {
            DiskStats diskStats = new DiskStats();
            diskStats.calculate(mainStatInfo);
        } else if (UserGroupsStats.STAT.equals(stat)) {
            UserGroupsStats userGroupsStats = new UserGroupsStats();
            userGroupsStats.calculate(session, mainStatInfo, statName);
        }
    }

    @Override
    public void calculateDocumentTypeTotals(CoreSession session, String[] doctypes, String path) {
        DocumentStats documentStats = new DocumentStats(doctypes, path);
        documentStats.runCompleteCalculation(session);
    }

    @Override
    public void calculateUsersAndGroupsTotals(CoreSession session) {
        UserGroupsStats userGroupsStats = new UserGroupsStats();
        userGroupsStats.runCompleteCalculation(session);
    }

    @Override
    public void calculateDiskTotals(CoreSession session) {
        DiskStats diskStats = new DiskStats();
        diskStats.runCompleteCalculation(session);
    }

    @Override
    public boolean isCalculationRunning() {
        WorkManager workManager = Framework.getService(WorkManager.class);
        List<Work> runningWorks = workManager.listWork("stats", Work.State.RUNNING);
        return runningWorks.size() > 0;
    }

    @Override
    public void reindexStat(CoreSession session, String statName, boolean forceAll, int batchSize, boolean refresh) {
        String nxql = null;
        if (statName == null) {
            LOG.error("Stat name is mandatory to reindex");
            return;
        }
        // Get query for stat
        nxql = getQueryForStat(statName);
        LOG.info("Query for reindex " + nxql);
        if (nxql != null) {
            if (forceAll) {
                LOG.info("Force all");
                reindexFromQuery(nxql);
            } else {
                LOG.info("Index with Removing...");
                List<String> docsToRemove = new ArrayList();
                DocumentModelList docsES = null;
                int page = 0;
                do {
                    Map<String, Object> params = new HashMap<>();
                    params.put("query", nxql);
                    params.put("limit", batchSize);
                    params.put("offset", page * batchSize);
                    try {
                        docsES = (DocumentModelList) UpgradeUtils.runOperation("Document.ElasticQuery", null, params, session);
                        List<String> idsDocsES = docsES.stream().map(DocumentModel::getId).collect(Collectors.toList());
                        String ids = String.join("','", idsDocsES);
                        LOG.info("ids =" + ids);
                        DocumentModelList sqlDocs = session.query("SELECT * FROM Document WHERE ecm:uuid IN ('" + ids + "')");
                        List<String> idsSQLDocs = sqlDocs.stream().map(DocumentModel::getId).collect(Collectors.toList());
                        LOG.info("Found ids: " + idsSQLDocs);
                        idsDocsES.removeAll(idsSQLDocs);
                        docsToRemove.addAll(idsDocsES);
                    } catch (Exception e) {
                        LOG.error("Unable to execute query ES", e);
                    }
                    page++;
                } while (docsES != null && !docsES.isEmpty());
                // Reindex worker for query
                reindexFromQuery(nxql);
                LOG.info("Docs to remove " + docsToRemove.size());
                removeDocumentFromElastic(session, docsToRemove);
            }
        }
        if (refresh) {
            StatInfo statInfo = RegisterStatInfo.getInstance().getStat(statName);
            if (statInfo != null) {
                RegisterStatInfo.getInstance().clearStat("Documents", statName);
                DocumentStats docs = new DocumentStats(new String[0], "/");
                String doctype = getDoctypeForStat(statName);
                if (doctype != null) {
                    docs.calculateForDoctype(session, statInfo, doctype, statName);
                } else {
                    docs.calculate(session, statInfo, statName);
                }
            }
        }
    }

    /**
     * Get query for stat.
     *
     * @param statName
     * @return
     */
    private String getQueryForStat(String statName) {
        String nxql = null;
        String doctype;
        if (statName.equals(DocumentStats.TOTAL_FOLDERS)) {
            nxql = "SELECT * FROM Folder WHERE ecm:mixinType = 'Folderish'";
        } else if (statName.equals(DocumentStats.TOTAL_ACTIVE_FOLDERS)) {
            nxql = "SELECT * FROM Folder WHERE " +
                    "ecm:mixinType != 'HiddenInNavigation' " +
                    "AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0";
        } else if (statName.equals(DocumentStats.TOTAL_FOLDERS_NOT_DELETED)) {
            nxql = "SELECT * FROM Folder WHERE " +
                    "ecm:currentLifeCycleState != 'deleted' " +
                    "AND ecm:mixinType != 'HiddenInNavigation' " +
                    "AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0";
        } else if (statName.equals(DocumentStats.TOTAL_ACTIVE_FILE_NOT_DELETE_WITH_CONTENT)) {
            nxql = "SELECT * FROM File WHERE file:content/length > 0 " +
                    "AND ecm:currentLifeCycleState != 'deleted' " +
                    "AND ecm:mixinType != 'HiddenInNavigation' " +
                    "AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0";
        } else if (statName.equals(DocumentStats.TOTAL_DOCUMENTS)) {
            nxql = "SELECT * FROM Document";
        } else if (statName.equals(DocumentStats.TOTAL_ACTIVE_DOCUMENTS)) {
            nxql = "SELECT * FROM Document WHERE " +
                    "ecm:mixinType != 'HiddenInNavigation' " +
                    "AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0";
        } else if (statName.equals(DocumentStats.TOTAL_DOCUMENTS_NOT_DELETED)) {
            nxql = "SELECT * FROM Document WHERE " +
                    "ecm:currentLifeCycleState != 'deleted' " +
                    "AND ecm:mixinType != 'HiddenInNavigation' " +
                    "AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0";
        } else if (statName.equals(DocumentStats.TOTAL_ACTIVE_DOCUMENTS_NOT_DELETED_WITH_CONTENT)) {
            nxql = "SELECT * FROM Document WHERE file:content/length > 0 " +
                    "AND ecm:currentLifeCycleState != 'deleted' " +
                    "AND ecm:mixinType != 'HiddenInNavigation' " +
                    "AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0";
        } else if (statName.startsWith(DocumentStats.TOTAL_FOR_DOCTYPE)) {
            doctype = statName.replace(DocumentStats.TOTAL_FOR_DOCTYPE + " ", "");
            nxql = "SELECT * FROM " + doctype;
        } else if (statName.startsWith(DocumentStats.TOTAL_ACTIVE_FOR_DOCTYPE)) {
            doctype = statName.replace(DocumentStats.TOTAL_ACTIVE_FOR_DOCTYPE + " ", "");
            nxql = "SELECT * FROM " + doctype + " WHERE " +
                    "ecm:mixinType != 'HiddenInNavigation' " +
                    "AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0";
        } else if (statName.startsWith(DocumentStats.TOTAL_ACTIVE_NOT_DELETED_FOR_DOCTYPE)) {
            doctype = statName.replace(DocumentStats.TOTAL_ACTIVE_NOT_DELETED_FOR_DOCTYPE + " ", "");
            nxql = "SELECT * FROM " + doctype + " WHERE " +
                    "ecm:currentLifeCycleState != 'deleted' " +
                    "AND ecm:mixinType != 'HiddenInNavigation' " +
                    "AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0";
        } else if (statName.startsWith(DocumentStats.TOTAL_ACTIVE_WITH_CONTENT_FOR_DOCTYPE)) {
            doctype = statName.replace(DocumentStats.TOTAL_ACTIVE_WITH_CONTENT_FOR_DOCTYPE + " ", "");
            nxql = "SELECT * FROM " + doctype + " WHERE file:content/length > 0 " +
                    "AND ecm:currentLifeCycleState != 'deleted' " +
                    "AND ecm:mixinType != 'HiddenInNavigation' " +
                    "AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0";
        }
        return nxql;
    }

    /**
     * Get doctype for stat.
     *
     * @param statName
     * @return
     */
    private String getDoctypeForStat(String statName) {
        String doctype = null;
        if (statName.startsWith(DocumentStats.TOTAL_FOR_DOCTYPE)) {
            doctype = statName.replace(DocumentStats.TOTAL_FOR_DOCTYPE + " ", "");
        } else if (statName.startsWith(DocumentStats.TOTAL_ACTIVE_FOR_DOCTYPE)) {
            doctype = statName.replace(DocumentStats.TOTAL_ACTIVE_FOR_DOCTYPE + " ", "");
        } else if (statName.startsWith(DocumentStats.TOTAL_ACTIVE_NOT_DELETED_FOR_DOCTYPE)) {
            doctype = statName.replace(DocumentStats.TOTAL_ACTIVE_NOT_DELETED_FOR_DOCTYPE + " ", "");
        } else if (statName.startsWith(DocumentStats.TOTAL_ACTIVE_WITH_CONTENT_FOR_DOCTYPE)) {
            doctype = statName.replace(DocumentStats.TOTAL_ACTIVE_WITH_CONTENT_FOR_DOCTYPE + " ", "");
        }
        return doctype;
    }

    private void reindexFromQuery(String query) {
        LOG.info("Reindex with query " + query);
        ElasticSearchIndexing esi = Framework.getService(ElasticSearchIndexing.class);
        esi.runReindexingWorker("default", query);
    }

    private void removeDocumentFromElastic(CoreSession session, List<String> docs) {
        ElasticSearchIndexing esi = Framework.getService(ElasticSearchIndexing.class);
        List<IndexingCommand> commands = new ArrayList<>();
        for (String docId : docs) {
            try {
                DocumentModel doc = session.getDocument(new IdRef(docId));
                IndexingCommand cmd = new IndexingCommand(doc, IndexingCommand.Type.DELETE, false, true);
                commands.add(cmd);
            } catch (DocumentNotFoundException e) {
                LOG.trace("Document " + docId + " is not found to delete from ES", e);
            }
        }
        esi.indexNonRecursive(commands);
    }

    private void checkAccess(Principal principal) {
        NuxeoPrincipal nxprincipal = (NuxeoPrincipal) principal;
        if (nxprincipal == null || ! nxprincipal.isAdministrator()) {
            throw new RuntimeException("Unauthorized access: " + principal);
        }
    }

    private void reindexDocument(Principal principal, DocumentModel doc) {
        checkAccess(principal);
        ElasticSearchIndexing esi = Framework.getService(ElasticSearchIndexing.class);
        IndexingCommand cmd = new IndexingCommand(doc, IndexingCommand.Type.DELETE, false, false);
        esi.runIndexingWorker(Arrays.asList(cmd));
        cmd = new IndexingCommand(doc, IndexingCommand.Type.INSERT, false, false);
        esi.runIndexingWorker(Arrays.asList(cmd));
    }
}
