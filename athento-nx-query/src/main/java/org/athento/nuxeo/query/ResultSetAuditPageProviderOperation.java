package org.athento.nuxeo.query;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.provider.ESDocumentAuditPageProvider;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.operations.services.AuditPageProviderOperation;
import org.nuxeo.ecm.automation.core.util.*;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntryList;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Elasticsearch query and fetch for Audit.
 */
@Operation(id = AuditPageProviderOperation.ID, category = Constants.CAT_FETCH, label = "Audit Query With Page Provider", description = "Perform "
        + "a query or a named provider query against Audit logs. Result is "
        + "paginated. The query result will become the input for the next "
        + "operation. If no query or provider name is given, a query based on default Audit page provider will be executed.", addToStudio = false, aliases = { "Audit.PageProvider" })
public class ResultSetAuditPageProviderOperation {

    private static final Log LOG = LogFactory.getLog(ResultSetAuditPageProviderOperation.class);

    public static final String ID = "Audit.QueryWithPageProvider";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    public static final String DESC = "DESC";

    public static final String ASC = "ASC";

    public static final String CMIS = "CMIS";

    @Context
    protected CoreSession session;

    @Context
    protected PageProviderService ppService;

    @Param(name = "providerName", required = false)
    protected String providerName;

    @Param(name = "language", required = false, widget = Constants.W_OPTION, values = {NXQL.NXQL, CMIS})
    protected String lang = NXQL.NXQL;

    @Param(name = "page", required = false)
    protected Integer page;

    @Param(name = "pageSize", required = false)
    protected Integer pageSize;

    /**
     * @deprecated since 6.0 use instead {@link #sortBy and @link #sortOrder}
     */
    @Deprecated
    @Param(name = "sortInfo", required = false)
    protected StringList sortInfoAsStringList;

    @Param(name = "queryParams", required = false)
    protected StringList strParameters;

    @Param(name = "namedQueryParams", required = false)
    protected Properties namedQueryParams;

    /**
     * @since 5.7
     */
    @Param(name = "maxResults", required = false)
    protected String maxResults = "100";

    /**
     * @since 6.0
     */
    @Param(name = "sortBy", required = false, description = "Sort by " + "properties (separated by comma)")
    protected String sortBy;

    /**
     * @since 6.0
     */
    @Param(name = "sortOrder", required = false, description = "Sort order, " + "ASC or DESC", widget = Constants.W_OPTION, values = {
            ASC, DESC})
    protected String sortOrder;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public Paginable<LogEntry> run() throws IOException {
        List<SortInfo> sortInfos = null;
        if (sortInfoAsStringList != null) {
            sortInfos = new ArrayList<SortInfo>();
            for (String sortInfoDesc : sortInfoAsStringList) {
                SortInfo sortInfo;
                if (sortInfoDesc.contains("|")) {
                    String[] parts = sortInfoDesc.split("|");
                    sortInfo = new SortInfo(parts[0], Boolean.parseBoolean(parts[1]));
                } else {
                    sortInfo = new SortInfo(sortInfoDesc, true);
                }
                sortInfos.add(sortInfo);
            }
        } else {
            // Sort Info Management
            if (!StringUtils.isBlank(sortBy)) {
                sortInfos = new ArrayList<>();
                String[] sorts = sortBy.split(",");
                String[] orders = null;
                if (!StringUtils.isBlank(sortOrder)) {
                    orders = sortOrder.split(",");
                }
                for (int i = 0; i < sorts.length; i++) {
                    String sort = sorts[i];
                    boolean sortAscending = (orders != null && orders.length > i && "asc".equals(orders[i].toLowerCase()));
                    sortInfos.add(new SortInfo(sort, sortAscending));
                }
            }
        }

        Object[] parameters = null;

        if (strParameters != null && !strParameters.isEmpty()) {
            parameters = strParameters.toArray(new String[strParameters.size()]);
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

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        if (providerName == null || providerName.length() == 0) {
            providerName = "AUDIT_BROWSER";
        }

        Long targetPage = null;
        if (page != null) {
            targetPage = page.longValue();
        }
        Long targetPageSize = null;
        if (pageSize != null) {
            targetPageSize = pageSize.longValue();
        }

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

        DocumentModel searchDoc = null;
        if (namedQueryParams != null && namedQueryParams.size() > 0) {
            String docType = ppService.getPageProviderDefinition(providerName).getWhereClause().getDocType();
            searchDoc = session.createDocumentModel(docType);
            setProperties(session, searchDoc, namedQueryParams);
        }

        ElasticSearchAuditProviderDescriptor desc = new ElasticSearchAuditProviderDescriptor();
        PageProvider<LogEntry> pp = (PageProvider<LogEntry>) ppService.getPageProvider("", desc, searchDoc,
                sortInfos, targetPageSize, targetPage, props, parameters);
        LogEntryList list = new LogEntryList(pp);
        return list;
    }

    public static void setProperties(CoreSession session, DocumentModel doc, org.nuxeo.ecm.automation.core.util.Properties properties)
            throws IOException, PropertyException {
        if (properties instanceof DataModelProperties) {
            DataModelProperties dataModelProperties = (DataModelProperties) properties;
            for (Map.Entry<String, Serializable> entry : dataModelProperties.getMap().entrySet()) {
                doc.setPropertyValue(entry.getKey(), entry.getValue());
            }
        } else {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                DocumentHelper.setProperty(session, doc, key, value);
            }
        }
    }
}