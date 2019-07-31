
package org.athento.nuxeo.module.workflow;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.provider.ESDocumentAuditPageProvider;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntryList;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Web object to manage workflows audit entries.
 *
 * @since NX 8.10
 */
@WebObject(type = "workflowEntries")
public class WorkflowEntriesObject extends DefaultObject {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(WorkflowEntriesObject.class);

    public static final String providerName = "DOCUMENT_WORKFLOW_HISTORY_PROVIDER";

    public static final String PAGE_SIZE = "pageSize";

    public static final String PAGE = "page";

    public static final String MAX_RESULTS = "maxResults";

    public static final String QUERY_PARAMS = "queryParams";

    public static final String PARAMETERS = "parameters";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    protected DocumentModel doc;

    protected EnumMap<WorkflowEntriesObject.QueryParams, String> queryParametersMap;

    protected PageProviderService pageProviderService;

    @Override
    public void initialize(Object... args) {
        if (args != null & args.length == 1) {
            doc = (DocumentModel) args[0];
        }
        pageProviderService = Framework.getLocalService(PageProviderService.class);
        // Query Enum Parameters Map
        queryParametersMap = new EnumMap<>(WorkflowEntriesObject.QueryParams.class);
        queryParametersMap.put(QueryParams.PAGE_SIZE, PAGE_SIZE);
        queryParametersMap.put(QueryParams.PAGE, PAGE);
        queryParametersMap.put(QueryParams.MAX_RESULTS, MAX_RESULTS);
        queryParametersMap.put(QueryParams.PARAMETERS, PARAMETERS);
        queryParametersMap.put(QueryParams.QUERY_PARAMS, QUERY_PARAMS);
    }

    @GET
    public LogEntryList doGet(@Context UriInfo uriInfo) {

        CoreSession session = ctx.getCoreSession();

        if (LOG.isInfoEnabled()) {
            LOG.info("Getting workflow audit entries...");
        }

        // Fetching all parameters
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String pageSize = queryParams.getFirst(PAGE_SIZE);
        String page = queryParams.getFirst(PAGE);
        String strParameters = queryParams.getFirst(PARAMETERS);
        StringList parameterList = new StringList();
        if (strParameters != null && !strParameters.isEmpty()) {
            parameterList = new StringList(strParameters.split(","));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Page info:" + page + ", " + pageSize + ", " + strParameters + ", " + queryParams);
        }

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        final class ElasticSearchAuditProviderDescriptor extends GenericPageProviderDescriptor {
            private static final long serialVersionUID = 1L;

            public ElasticSearchAuditProviderDescriptor() {
                super();
                try {
                    klass = (Class<PageProvider<?>>) Class.forName(ESDocumentAuditPageProvider.class.getName());
                } catch (ClassNotFoundException e) {
                    LOG.error(e, e);
                }
            }
        }

        Object[] parameters = null;
        if (strParameters != null && !strParameters.isEmpty()) {
            parameters = parameterList.toArray(new String[parameterList.size()]);
            // expand specific parameters
            for (int idx = 0; idx < parameters.length; idx++) {
                String value = (String) parameters[idx];
                if (value.equals(CURRENT_USERID_PATTERN)) {
                    parameters[idx] = session.getPrincipal().getName();
                } else if (value.equals(CURRENT_REPO_PATTERN)) {
                    parameters[idx] = session.getRepositoryName();
                }
            }
        }

        Map<String, String> params = new HashMap<>();
        String queryParamsString = queryParams.getFirst(QUERY_PARAMS);
        if (queryParamsString != null) {
            String [] splittedStr = queryParamsString.split(",");
            for (String splitted : splittedStr) {
                String [] kvs = splitted.split("=");
                params.put(kvs[0], kvs[1]);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Params=" + params);
        }

        String docType = pageProviderService.getPageProviderDefinition(providerName).getWhereClause().getDocType();
        DocumentModel searchDoc = session.createDocumentModel(docType);
        try {
            setProperties(session, searchDoc, params);
        } catch (IOException e) {
            LOG.error("Set property error to find audit workflow entries", e);
        }

        ElasticSearchAuditProviderDescriptor desc = new ElasticSearchAuditProviderDescriptor();
        PageProvider<LogEntry> pp = (PageProvider<LogEntry>) pageProviderService.getPageProvider("", desc, searchDoc,
                new ArrayList<>(0), Long.valueOf(pageSize), Long.valueOf(page), props, parameters);
        LogEntryList list = new LogEntryList(pp);
        return list;
    }

    /**
     * Set properties to search.
     *
     * @param session
     * @param doc
     * @param properties
     * @throws IOException
     * @throws PropertyException
     */
    public static void setProperties(CoreSession session, DocumentModel doc, Map<String, String> properties)
            throws IOException, PropertyException {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            DocumentHelper.setProperty(session, doc, key, value);
        }
    }

    public enum QueryParams {
        PAGE_SIZE, PAGE, MAX_RESULTS, PARAMETERS, QUERY_PARAMS
    }


}
