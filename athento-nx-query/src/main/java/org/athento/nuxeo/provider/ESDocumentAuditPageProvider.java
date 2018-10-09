package org.athento.nuxeo.provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.document.AdditionalDocumentAuditParams;
import org.nuxeo.ecm.platform.audit.api.document.DocumentAuditHelper;
import org.nuxeo.elasticsearch.audit.pageprovider.ESAuditPageProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ElasticSearch provider to get workflow history.
 */
public class ESDocumentAuditPageProvider extends ESAuditPageProvider {

    private static final long serialVersionUID = 1L;

    protected Log LOG = LogFactory.getLog(ESDocumentAuditPageProvider.class);

    protected Object[] newParams;

    protected static String singleQuery = "            {\n"
            + "                \"bool\" : {\n"
            + "                  \"must\" : {\n"
            + "                    \"match\" : {\n"
            + "                      \"docUUID\" : {\n"
            + "                        \"query\" : \"?\",\n"
            + "                        \"type\" : \"boolean\"\n"
            + "                      }\n" + "                    }\n"
            + "                  }\n" + "                }\n"
            + "              }          \n" + "";

    @Override
    public List<LogEntry> getCurrentPage() {
        LOG.info("Get current page for entries...");
        String uuid = (String) getParameters()[0];
        Map<String, FilterMapEntry> params = new HashMap<>();
        DocumentModel searchDoc = getSearchDocumentModel();
        if (searchDoc != null) {
            String category = (String) searchDoc.getPropertyValue("bas:eventCategory");
            if (category != null && !category.isEmpty()) {
                FilterMapEntry mapEntry = new FilterMapEntry();
                mapEntry.setColumnName("category");
                mapEntry.setObject(category);
                params.put("category", mapEntry);
            }
            String eventId = (String) searchDoc.getPropertyValue("bas:eventId");
            if (eventId != null && !eventId.isEmpty()) {
                FilterMapEntry mapEntry = new FilterMapEntry();
                mapEntry.setColumnName("eventId");
                mapEntry.setObject(eventId);
                params.put("eventId", mapEntry);
            }
            String principalName = (String) searchDoc.getPropertyValue("bas:principalName");
            if (principalName != null && !principalName.isEmpty()) {
                FilterMapEntry mapEntry = new FilterMapEntry();
                mapEntry.setColumnName("principalName");
                mapEntry.setObject(principalName);
                params.put("principalName", mapEntry);
            }
        }
        for (Map.Entry<String, FilterMapEntry> i : params.entrySet()) {
            LOG.info("Go executing with param " + i.getValue().getColumnName() + "=" + i.getValue().getObject() + " and uuid " + uuid);
        }
        List<LogEntry> entries =  getESBackend().getLogEntriesFor(uuid, params, true);
        this.setResultsCount(entries.size());
        //preprocessCommentsIfNeeded(entries);
        return entries;
    }

    @Override
    protected String getFixedPart() {
        return singleQuery;
    }

    @Override
    public List<SortInfo> getSortInfos() {
        List<SortInfo> sort = super.getSortInfos();
        if (sort == null || sort.size() == 0) {
            sort = new ArrayList<SortInfo>();
            sort.add(new SortInfo("eventDate", true));
            sort.add(new SortInfo("id", true));
        }
        return sort;
    }

    // It is able to use with filter.
    @Override
    public Object[] getParameters() {
        if (newParams == null) {
            Object[] params = super.getParameters();
            if (params == null || params.length != 1) {
                log.error(this.getClass().getSimpleName()
                        + " Expect only one parameter the document uuid, unexpected behavior may occur");
                return newParams;
            }
            CoreSession session;
            String uuid;
            if (params[0] instanceof DocumentModel) {
                DocumentModel doc = (DocumentModel) params[0];
                uuid = doc.getId();
                session = doc.getCoreSession();
            } else {
                session = (CoreSession) getProperties().get(
                        CORE_SESSION_PROPERTY);
                uuid = params[0].toString();
            }
            if (session != null) {
                try {
                    AdditionalDocumentAuditParams additionalParams = DocumentAuditHelper.getAuditParamsForUUID(
                            uuid, session);
                    if (additionalParams != null) {
                        newParams = new Object[] { uuid,
                                additionalParams.getTargetUUID(),
                                additionalParams.getMaxDate() };
                    } else {
                        newParams = new Object[] { uuid };
                    }
                } catch (Exception e) {
                    log.error(
                            "Error while fetching additional parameters for audit query",
                            e);
                }
            } else {
                log.warn("No core session found: cannot compute all info to get complete audit entries");
                return params;
            }
        }
        return newParams;
    }

    @Override
    public boolean hasChangedParameters(Object[] parameters) {
        return getParametersChanged(this.parameters, parameters);
    }

}
