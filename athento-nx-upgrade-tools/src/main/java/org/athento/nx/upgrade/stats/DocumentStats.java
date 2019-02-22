package org.athento.nx.upgrade.stats;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.api.DocumentInfo;
import org.athento.nx.upgrade.util.UpgradeUtils;
import org.nuxeo.ecm.automation.core.util.PaginableRecordSet;
import org.nuxeo.ecm.core.api.CoreSession;

import java.util.HashMap;
import java.util.Map;

/**
 * Document stats.
 */
public class DocumentStats implements StatCalculation {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(DocumentStats.class);

    public static final String STAT = "Documents";

    public static final String TOTAL_FOLDERS = "Total Folders";
    public static final String TOTAL_ACTIVE_FOLDERS = "Total active Folders";
    public static final String TOTAL_FOLDERS_NOT_DELETED = "Total Folders not deleted";
    public static final String TOTAL_ACTIVE_FILE_NOT_DELETE_WITH_CONTENT = "Total active File not deleted with content";
    public static final String TOTAL_DOCUMENTS = "Total Documents";
    public static final String TOTAL_ACTIVE_DOCUMENTS = "Total active Documents";
    public static final String TOTAL_DOCUMENTS_NOT_DELETED = "Total Documents not deleted";
    public static final String TOTAL_ACTIVE_DOCUMENTS_NOT_DELETED_WITH_CONTENT = "Total active File not deleted with content";
    public static final String TOTAL_FOR_DOCTYPE = "Total for";
    public static final String TOTAL_ACTIVE_FOR_DOCTYPE = "Totals active for";
    public static final String TOTAL_ACTIVE_NOT_DELETED_FOR_DOCTYPE = "Totals active not deleted for";
    public static final String TOTAL_ACTIVE_WITH_CONTENT_FOR_DOCTYPE ="Totals active with content for";

    String [] doctypes;
    private String path;

    public DocumentStats() {

    }

    public DocumentStats(String path) {
        this.path = path;
    }

    public DocumentStats(String [] doctypes, String path) {
        this.doctypes = doctypes;
        this.path = path;
    }

    /**
     * Run total calculations for document stats.
     *
     * @param session
     */
    @Override
    public void runCompleteCalculation(CoreSession session) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Run calculation stats for Documents...");
        }

        RegisterStatInfo statsInfo = RegisterStatInfo.getInstance();
        // Clear current stats
        statsInfo.clear("Documents");

        StatInfo documentStats = new StatInfo("Documents", "Documents in repository");
        statsInfo.getStats().add(documentStats);

        calculate(session, documentStats, TOTAL_FOLDERS);
        calculate(session, documentStats, TOTAL_ACTIVE_FOLDERS);
        calculate(session, documentStats, TOTAL_FOLDERS_NOT_DELETED);
        calculate(session, documentStats, TOTAL_DOCUMENTS);
        calculate(session, documentStats, TOTAL_ACTIVE_DOCUMENTS);
        calculate(session, documentStats, TOTAL_DOCUMENTS_NOT_DELETED);
        calculate(session, documentStats, TOTAL_ACTIVE_FILE_NOT_DELETE_WITH_CONTENT);

        if (doctypes.length > 0) {
            for (String doctype : doctypes) {
                calculateForDoctype(session, documentStats, doctype, TOTAL_FOR_DOCTYPE);
                calculateForDoctype(session, documentStats, doctype, TOTAL_ACTIVE_FOR_DOCTYPE);
                calculateForDoctype(session, documentStats, doctype, TOTAL_ACTIVE_NOT_DELETED_FOR_DOCTYPE);
                calculateForDoctype(session, documentStats, doctype, TOTAL_ACTIVE_WITH_CONTENT_FOR_DOCTYPE);
            }
        }
    }

    /**
     * Calculate for doctype.
     *
     * @param session
     * @param statInfo
     * @param doctype
     * @param statName
     */
    public void calculateForDoctype(CoreSession session, StatInfo statInfo, String doctype, String statName) {
        RegisterStatInfo registerStatInfo = RegisterStatInfo.getInstance();
        if (statInfo == null) {
            statInfo = new StatInfo("Documents", "Documents in repository");
            registerStatInfo.getStats().add(statInfo);
        } else {
            statInfo = registerStatInfo.getStat(statInfo.getName());
            if (statInfo != null) {
                registerStatInfo.clearStat(statInfo.getName(), statName);
            }
        }
        statName = statName.replace(doctype, "").trim();
        LOG.info("Calculation for doctype " + doctype + ", " + statName);
        if (TOTAL_FOR_DOCTYPE.equals(statName)) {
            Long totalDoctype = getTotalForDoctype(session, doctype, null, path, false, false, false);
            Long totalIndexedDoctype = getTotalIndexed(session, doctype, null, path, false, false, false);
            DocumentInfo documentAddocInfo = new DocumentInfo(doctype, path);
            documentAddocInfo.setName(TOTAL_FOR_DOCTYPE + " " + doctype);
            documentAddocInfo.setDescription("Number of documents of type " + doctype + " into repository, including proxies, hidden and versions");
            documentAddocInfo.setTotalDocuments(totalDoctype);
            documentAddocInfo.setIndexedDocuments(totalIndexedDoctype);
            documentAddocInfo.setAll(true);
            statInfo.getEntries().add(0, documentAddocInfo);
        } else if (TOTAL_ACTIVE_FOR_DOCTYPE.equals(statName)) {
            Long totalDoctypeActive = getTotalForDoctype(session, doctype, null, path, true, false, false);
            Long totalIndexedDoctypeActive = getTotalIndexed(session, doctype, null, path, true, false, false);
            DocumentInfo documentAddocInfoActive = new DocumentInfo(doctype, path);
            documentAddocInfoActive.setName(TOTAL_ACTIVE_FOR_DOCTYPE + " " + doctype);
            documentAddocInfoActive.setDescription("Number of active documents of type " + doctype + " into repository");
            documentAddocInfoActive.setTotalDocuments(totalDoctypeActive);
            documentAddocInfoActive.setIndexedDocuments(totalIndexedDoctypeActive);
            documentAddocInfoActive.setAll(false);
            statInfo.getEntries().add(0, documentAddocInfoActive);
        } else if (TOTAL_ACTIVE_NOT_DELETED_FOR_DOCTYPE.equals(statName)) {
            Long totalDoctypeActiveNotDeleted = getTotalForDoctype(session, doctype, null, path, true, false, true);
            Long totalIndexedDoctypeActiveNotDeleted = getTotalIndexed(session, doctype, null, path, true, false, true);
            DocumentInfo documentAddocInfoActiveNotDeleted = new DocumentInfo(doctype, path);
            documentAddocInfoActiveNotDeleted.setName(TOTAL_ACTIVE_NOT_DELETED_FOR_DOCTYPE + " " + doctype);
            documentAddocInfoActiveNotDeleted.setDescription("Number of active not deleted documents of type " + doctype + " into repository");
            documentAddocInfoActiveNotDeleted.setTotalDocuments(totalDoctypeActiveNotDeleted);
            documentAddocInfoActiveNotDeleted.setIndexedDocuments(totalIndexedDoctypeActiveNotDeleted);
            documentAddocInfoActiveNotDeleted.setAll(false);
            statInfo.getEntries().add(0, documentAddocInfoActiveNotDeleted);
        } else if (TOTAL_ACTIVE_WITH_CONTENT_FOR_DOCTYPE.equals(statName)) {
            Long totalDoctypeActiveWithContent = getTotalForDoctype(session, doctype, null, path, true, true, false);
            Long totalIndexedDoctypeActiveWithContent = getTotalIndexed(session, doctype, null, path, true, true, false);
            DocumentInfo documentAddocInfoActiveWithContent = new DocumentInfo(doctype, path);
            documentAddocInfoActiveWithContent.setName(TOTAL_ACTIVE_WITH_CONTENT_FOR_DOCTYPE + " " + doctype);
            documentAddocInfoActiveWithContent.setDescription("Number of active documents of type " + doctype + " where his content is not empty");
            documentAddocInfoActiveWithContent.setTotalDocuments(totalDoctypeActiveWithContent);
            documentAddocInfoActiveWithContent.setIndexedDocuments(totalIndexedDoctypeActiveWithContent);
            documentAddocInfoActiveWithContent.setAll(false);
            statInfo.getEntries().add(0, documentAddocInfoActiveWithContent);
        }  else {
            LOG.warn("No calculation for statName " + statName);
        }
    }

    /**
     * Calculate generic.
     *
     * @param session
     * @param statInfo
     * @param statName
     */
    public void calculate(CoreSession session, StatInfo statInfo, String statName) {
        RegisterStatInfo registerStatInfo = RegisterStatInfo.getInstance();
        if (statInfo == null) {
            statInfo = new StatInfo("Documents", "Documents in repository");
            registerStatInfo.getStats().add(statInfo);
        } else {
            statInfo = registerStatInfo.getStat(statInfo.getName());
            if (statInfo != null) {
                registerStatInfo.clearStat(statInfo.getName(), statName);
            }
        }
        LOG.info("Calculating " + statName);
        if (TOTAL_FOLDERS.equals(statName)) {
            Long totalFolders = getTotalForDoctype(session, null, "'Folderish'", path, false, false, false);
            Long totalIndexedFolders = getTotalIndexed(session, null, "'Folderish'", path, false, false, false);
            DocumentInfo folderInfo = new DocumentInfo("Folder", path);
            folderInfo.setName(TOTAL_FOLDERS);
            folderInfo.setDescription("Number of folder in repository, including proxies, hidden and versions");
            folderInfo.setTotalDocuments(totalFolders);
            folderInfo.setIndexedDocuments(totalIndexedFolders);
            folderInfo.setAll(true);
            statInfo.getEntries().add(0, folderInfo);
        } else if (TOTAL_ACTIVE_FOLDERS.equals(statName)) {
            Long totalFoldersActive = getTotalForDoctype(session, null, "'Folderish'", path, true, false, false);
            Long totalIndexedFoldersActive = getTotalIndexed(session, null, "'Folderish'", path, true, false, false);
            DocumentInfo folderInfoActive = new DocumentInfo("Folder", path);
            folderInfoActive.setName(TOTAL_ACTIVE_FOLDERS);
            folderInfoActive.setDescription("Number of active folder into repository");
            folderInfoActive.setTotalDocuments(totalFoldersActive);
            folderInfoActive.setIndexedDocuments(totalIndexedFoldersActive);
            folderInfoActive.setAll(false);
            statInfo.getEntries().add(0, folderInfoActive);
        } else if (TOTAL_FOLDERS_NOT_DELETED.equals(statName)) {
            Long totalFoldersActiveNotDeleted = getTotalForDoctype(session, null, "'Folderish'", path, true, false, true);
            Long totalIndexedFoldersActiveNotDeleted = getTotalIndexed(session, null, "'Folderish'", path, true, false, true);
            DocumentInfo folderInfoActiveNotDeleted = new DocumentInfo("Folder", path);
            folderInfoActiveNotDeleted.setName(TOTAL_FOLDERS_NOT_DELETED);
            folderInfoActiveNotDeleted.setDescription("Number of active folder not deleted into repository");
            folderInfoActiveNotDeleted.setTotalDocuments(totalFoldersActiveNotDeleted);
            folderInfoActiveNotDeleted.setIndexedDocuments(totalIndexedFoldersActiveNotDeleted);
            folderInfoActiveNotDeleted.setAll(false);
            statInfo.getEntries().add(0, folderInfoActiveNotDeleted);
        } else if (TOTAL_DOCUMENTS.equals(statName)) {
            Long totalDocuments = getTotalForDoctype(session, null, null, path, false, false, false);
            Long totalIndexedDocuments = getTotalIndexed(session, null, null, path, false, false, false);
            DocumentInfo documentInfo = new DocumentInfo("Document", path);
            documentInfo.setName(TOTAL_DOCUMENTS);
            documentInfo.setDescription("Number of documents into repository, including proxies, hidden and versions");
            documentInfo.setTotalDocuments(totalDocuments);
            documentInfo.setIndexedDocuments(totalIndexedDocuments);
            documentInfo.setAll(true);
            statInfo.getEntries().add(0, documentInfo);
        } else if (TOTAL_ACTIVE_DOCUMENTS.equals(statName)) {
            Long totalDocumentsActive = getTotalForDoctype(session, null, null, path, true, false, false);
            Long totalIndexedDocumentsActive = getTotalIndexed(session, null, null, path, true, false, false);
            DocumentInfo documentInfoActive = new DocumentInfo("Document", path);
            documentInfoActive.setName(TOTAL_ACTIVE_DOCUMENTS);
            documentInfoActive.setDescription("Number of active documents into repository");
            documentInfoActive.setTotalDocuments(totalDocumentsActive);
            documentInfoActive.setIndexedDocuments(totalIndexedDocumentsActive);
            documentInfoActive.setAll(false);
            statInfo.getEntries().add(0, documentInfoActive);
        } else if (TOTAL_DOCUMENTS_NOT_DELETED.equals(statName)) {
            Long totalDocumentsActiveNotDeleted = getTotalForDoctype(session, null, null, path, true, false, true);
            Long totalIndexedDocumentsActiveNotDeleted = getTotalIndexed(session, null, null, path, true, false, true);
            DocumentInfo documentInfoActiveNotDeleted = new DocumentInfo("Document", path);
            documentInfoActiveNotDeleted.setName(TOTAL_DOCUMENTS_NOT_DELETED);
            documentInfoActiveNotDeleted.setDescription("Number of active documents not deleted into repository");
            documentInfoActiveNotDeleted.setTotalDocuments(totalDocumentsActiveNotDeleted);
            documentInfoActiveNotDeleted.setIndexedDocuments(totalIndexedDocumentsActiveNotDeleted);
            documentInfoActiveNotDeleted.setAll(false);
            statInfo.getEntries().add(0, documentInfoActiveNotDeleted);
        } else if (TOTAL_ACTIVE_FILE_NOT_DELETE_WITH_CONTENT.equals(statName)) {
            Long totalFileActiveNotDeleted = getTotalForDoctype(session, "File", null, path, true, true, true);
            Long totalIndexedFileActiveNotDeleted = getTotalIndexed(session, "File", null, path, true, true, true);
            DocumentInfo fileInfoActiveNotDeleted = new DocumentInfo("Document", path);
            fileInfoActiveNotDeleted.setName(TOTAL_ACTIVE_FILE_NOT_DELETE_WITH_CONTENT);
            fileInfoActiveNotDeleted.setDescription("Number of active File not deleted into repository where his content is not empty");
            fileInfoActiveNotDeleted.setTotalDocuments(totalFileActiveNotDeleted);
            fileInfoActiveNotDeleted.setIndexedDocuments(totalIndexedFileActiveNotDeleted);
            fileInfoActiveNotDeleted.setAll(false);
            statInfo.getEntries().add(0, fileInfoActiveNotDeleted);
        }
    }

    /**
     * Get total for a document type and path.
     *
     * @param session
     * @param doctype
     * @param facets
     * @param path
     * @param active is no deleted, no proxies
     * @return
     */
    protected Long getTotalForDoctype(CoreSession session, String doctype, String facets, String path, boolean active, boolean withContent, boolean checkDeleted) {
        if (doctype == null) {
            doctype = "Document";
        }
        String nxqlQuery = "SELECT ecm:uuid FROM "  + doctype;
        if (facets != null || path != null || withContent || checkDeleted  || active) {
            nxqlQuery += " WHERE ";
        }
        if (facets != null) {
            nxqlQuery += " ecm:mixinType IN (" + facets + ")";
        }
        if (path != null) {
            nxqlQuery += " " + (facets != null ? " AND " : "") + "ecm:path STARTSWITH '" + path + "'";
        }
        if (withContent) {
            nxqlQuery += " " + (facets != null || path != null ? " AND " : "") + " file:content/length > 0 ";
        }
        if (checkDeleted) {
            nxqlQuery += " " + (facets != null || path != null || withContent ? " AND " : "") + " ecm:currentLifeCycleState != 'deleted' ";
        }
        if (active) {
            nxqlQuery += " " + (facets != null || path != null || withContent || checkDeleted ? " AND " : "") + " ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0";
        }
        Map<String, Object> params = new HashMap<>();
        params.put("query", nxqlQuery);
        try {
            PaginableRecordSet res = (PaginableRecordSet) UpgradeUtils.runOperation("Resultset.PageProvider", null, params, session);
            if (res != null) {
                LOG.info("**** Total for " + doctype + " with query " + nxqlQuery + "=" + res.getResultsCount());
                return res.getResultsCount();
            }
        } catch (Exception e) {
            LOG.error("Unable to get total documents for " + doctype, e);
        }
        return 0L;
    }

    /**
     * Get total for a document type and path.
     *
     * @param session
     * @param doctype
     * @param facets
     * @param path
     * @param active is no deleted, no proxies
     * @return
     */
    protected Long getTotalIndexed(CoreSession session, String doctype, String facets, String path, boolean active, boolean withContent, boolean checkDeleted) {
        if (doctype == null) {
            doctype = "Document";
        }
        String nxqlQuery = "SELECT ecm:uuid FROM "  + doctype;
        if (facets != null || path != null || withContent || checkDeleted  || active) {
            nxqlQuery += " WHERE ";
        }
        if (facets != null) {
            nxqlQuery += " ecm:mixinType IN (" + facets + ")";
        }
        if (path != null) {
            nxqlQuery += " " + (facets != null ? " AND " : "") + "ecm:path STARTSWITH '" + path + "'";
        }
        if (withContent) {
            nxqlQuery += " " + (facets != null || path != null ? " AND " : "") + " file:content/length > 0 ";
        }
        if (checkDeleted) {
            nxqlQuery += " " + (facets != null || path != null || withContent ? " AND " : "") + " ecm:currentLifeCycleState != 'deleted' ";
        }
        if (active) {
            nxqlQuery += " " + (facets != null || path != null || withContent || checkDeleted ? " AND " : "") + " ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0";
        }
        Map<String, Object> params = new HashMap<>();
        params.put("query", nxqlQuery);
        params.put("pageSize", 100);
        try {
            PaginableRecordSet res = (PaginableRecordSet) UpgradeUtils.runOperation("Athento.DocumentResultSet", null, params, session);
            if (res != null) {
                LOG.info("**** Total for " + doctype + " with query " + nxqlQuery + "=" + res.getResultsCount());
                return res.getResultsCount();
            }
        } catch (Exception e) {
            LOG.error("Unable to get total indexed for " + doctype, e);
        }
        return 0L;
    }

}
