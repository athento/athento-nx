package org.athento.nuxeo.provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.audit.ESAthentoAuditBackend;

import org.athento.nuxeo.query.QueryUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.document.AdditionalDocumentAuditParams;
import org.nuxeo.ecm.platform.audit.api.document.DocumentAuditHelper;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.elasticsearch.audit.pageprovider.ESAuditPageProvider;
import org.nuxeo.runtime.api.Framework;

import java.util.*;

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
        LOG.info("Get current page for workflow audit entries...");
        Object [] parameters = getParameters();
        String uuid = null;
        if (parameters != null && parameters.length > 0) {
            uuid = (String) getParameters()[0];
        }
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
            GregorianCalendar startDate = (GregorianCalendar) searchDoc.getPropertyValue("bas:startDate");
            GregorianCalendar endDate = (GregorianCalendar) searchDoc.getPropertyValue("bas:endDate");
            if (startDate != null && endDate != null) {
                FilterMapEntry mapEntry = new FilterMapEntry();
                mapEntry.setColumnName("eventDate");
                String sDate = QueryUtils.formatDate(startDate.getTime());
                String eDate = QueryUtils.formatDate(endDate.getTime());
                mapEntry.setObject(sDate + "|" + eDate);
                params.put("eventDate", mapEntry);
            }
        }
        for (Map.Entry<String, FilterMapEntry> i : params.entrySet()) {
            LOG.debug("Go executing with param " + i.getValue().getColumnName() + "=" + i.getValue().getObject() + " and uuid " + uuid);
        }
        List<LogEntry> entries;
        if (uuid != null && !uuid.isEmpty()) {
            entries = getESBackend().getLogEntriesFor(uuid, params, true);
        } else {
            entries = getESBackend().getLogEntries(params, true);
        }
        this.setResultsCount(entries.size());
        int pageSize = (int) getPageSize();
        int page = (int) getCurrentPageIndex();
        int offset = page * pageSize;
        int end = offset + pageSize;
        if (offset < entries.size()) {
            if (end > entries.size()) {
                end = entries.size();
            }
            entries = entries.subList(offset, end);
        } else {
            entries = Collections.emptyList();
        }
        //preprocessCommentsIfNeeded(entries);
        return entries;
    }

    @Override
    protected ESAthentoAuditBackend getESBackend() {
        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime().getComponent(NXAuditEventsService.NAME);
        AuditBackend backend = audit.getBackend();
        LOG.info("Backend class " + backend.getClass());
        if (backend instanceof ESAthentoAuditBackend) {
            return (ESAthentoAuditBackend)backend;
        } else {
            throw new NuxeoException("Unable to use Athento ESAuditPageProvider if audit service is not configured to run with ElasticSearch");
        }
    }

    @Override
    protected String getFixedPart() {
        return singleQuery;
    }

    @Override
    public List<SortInfo> getSortInfos() {
        List<SortInfo> sort = super.getSortInfos();
        if (sort == null || sort.size() == 0) {
            sort = new ArrayList<>();
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
