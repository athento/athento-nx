package org.athento.nuxeo.query;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.provider.ElasticSearchQueryProviderDescriptor;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.operations.services.PaginableRecordSetImpl;
import org.nuxeo.ecm.automation.core.util.RecordSet;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Elasticsearch query and fetch operation based on ResultSetPageProvider of Nuxeo6.
 * <p/>
 * Created by victorsanchez on 5/5/16.
 */
@Operation(id = ResultSetElasticPageProviderOperation.ID, category = Constants.CAT_FETCH, label = "Elasticsearch QueryAndFetch", description = "Perform "
        + "a elasticsearch query or a named provider query on the repository. Result is "
        + "paginated. The result is returned as a RecordSet (QueryAndFetch) "
        + "rather than as a List of Document"
        + "The query result will become the input for the next "
        + "operation. If no query or provider name is given, a query returning "
        + "all the documents that the user has access to will be executed.", addToStudio = false)
public class ResultSetElasticPageProviderOperation {

    private static final Log LOG = LogFactory.getLog(ResultSetElasticPageProviderOperation.class);

    public static final String ID = "Resultset.PageProvider";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    public static final String DESC = "DESC";

    public static final String ASC = "ASC";

    public static final String CMIS = "CMIS";

    @Context
    protected CoreSession session;

    @Context
    protected PageProviderService ppService;

    @Param(name = "query", required = false)
    protected String query;

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

    @Param(name = "fieldList", required = false)
    protected StringList fieldList;

    @Param(name = "fieldComplex", required = false)
    protected StringList fieldComplex;

    @Param(name = "showCastingSource", required = false)
    protected boolean showCastingSource = false;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public RecordSet run() throws OperationException, IOException {
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

        if (query == null) {
            // provide a defaut query
            query = "SELECT * from Document";
        }

        Long targetPage = null;
        if (page != null) {
            targetPage = page.longValue();
        }
        Long targetPageSize = null;
        if (pageSize != null) {
            targetPageSize = pageSize.longValue();
        }


        // Prepare descriptor provider
        ElasticSearchQueryProviderDescriptor desc = new ElasticSearchQueryProviderDescriptor();
        if (maxResults != null && !maxResults.isEmpty() && !maxResults.equals("-1")) {
            // set the maxResults to avoid slowing down queries
            desc.getProperties().put("maxResults", maxResults);
        }
        // Manage queries
        PageProvider<Map<String, Serializable>> pp = null;
        if (query != null) {
            QueryContext queryCtxt;
            // Manage multiple queries
            if (query.contains(QueryUtils.QUERY_SEPARATOR)) {
                ArrayList<String> queries = QueryUtils.extractQueriesFromQuery(query, "\\" + QueryUtils.QUERY_SEPARATOR);
                queryCtxt = new QueryContext(queries, page * pageSize, pageSize, sortInfos);
                // Execute queries
                QueryUtils.executeRecursiveResultset(ppService, queryCtxt, props, parameters, false);
                // Set last query
                desc.setPattern(queryCtxt.getQuery());
                pp = (ElasticSearchQueryAndFetchPageProvider) ppService.getPageProvider("", desc, null, sortInfos,
                        targetPageSize, targetPage, props, parameters);
            } else {
                desc.setPattern(query);
                pp = (ElasticSearchQueryAndFetchPageProvider) ppService.getPageProvider("", desc, null, sortInfos,
                        targetPageSize, targetPage, props, parameters);
            }
        }
        if (pp != null) {
            // Check casting fields
            manageCastFields(pp);
            // Check field list items
            if (hasFieldList()) {
                LOG.info("fieldList param is deprecated. Please, use ::list casting in your SELECT clause");
                // Manage fields for get its field items
                manageFieldList(pp);
            }
            // Check field complex items
            if (hasFieldComplex()) {
                LOG.info("complexField param is deprecated. Please, use ::complex casting in your SELECT clause");
                manageFieldComplex(pp);
            }
        }
        PaginableRecordSetImpl res = new PaginableRecordSetImpl(pp);
        if (res.hasError()) {
            throw new OperationException(res.getErrorMessage());
        }
        return res;

    }

    /**
     * Manage casting in fields.
     *
     * @param pp
     */
    private void manageCastFields(PageProvider<Map<String, Serializable>> pp) {
        for (Map<String, Serializable> item : pp.getCurrentPage()) {
            if (item.get("ecm:uuid") == null) {
                continue;
            }
            Map<String, Serializable> newItems = new HashMap<>();
            for (Map.Entry<String, Serializable> field : item.entrySet()) {
                String metadata = field.getKey();
                if (QueryUtils.hasCasting(metadata)) {
                    DocumentRef idRef = new IdRef((String) item.get("ecm:uuid"));
                    try {
                        DocumentModel doc = session.getDocument(idRef);
                        CastField[] castedValues = QueryUtils.cast(metadata, doc);
                        int fieldPos = 0;
                        for (CastField castedValue : castedValues) {
                            if (castedValue.isOriginal() && showCastingSource) {
                                newItems.put(castedValue.getField(), castedValue.getValue());
                            } else {
                                newItems.put(castedValue.getField() + "_" + fieldPos, castedValue.getValue());
                            }
                        }
                    } catch (DocumentNotFoundException e) {
                        LOG.warn("Document " + idRef + " is not found in Athento, no casting is doing.");
                    }
                } else {
                    newItems.put(metadata, field.getValue());
                }
            }
            item.clear();
            item.putAll(newItems);
        }
    }

    /**
     * Manage field complex.
     */
    private void manageFieldComplex(PageProvider<Map<String, Serializable>> pp) {
        for (Map<String, Serializable> item : pp.getCurrentPage()) {
            if (item.get("ecm:uuid") != null) {
                try {
                    DocumentModel doc = session.getDocument(new IdRef((String) item.get("ecm:uuid")));
                    for (String field : fieldComplex) {
                        if (field != null) {
                            field = field.trim();
                            if (!field.isEmpty()) {
                                try {
                                    String aux = field;
                                    if (aux.contains("/")) {
                                        aux = field.split("/")[0];
                                    }
                                    Property prop = doc.getProperty(aux);
                                    if (prop.isComplex()) {
                                        Serializable value = doc.getPropertyValue(field);
                                        item.put(field, value);
                                    } else {
                                        LOG.warn("Field " + field + " is not complex");
                                    }
                                } catch (PropertyNotFoundException e) {
                                    LOG.trace("Ignore document property " + field + " for " + doc.getId());
                                }
                            }
                        }
                    }
                } catch (NuxeoException e) {
                    LOG.trace("Document is not found into ResultSet " + item.get("ecm:uuid"));
                }
            } else {
                // Checking ecm:uuid to get list information
                return;
            }
        }
    }

    /**
     * Manage field list.
     */
    private void manageFieldList(PageProvider<Map<String, Serializable>> pp) {
        for (Map<String, Serializable> item : pp.getCurrentPage()) {
            if (item.get("ecm:uuid") != null) {
                try {
                    DocumentModel doc = session.getDocument(new IdRef((String) item.get("ecm:uuid")));
                    for (String field : fieldList) {
                        if (field != null) {
                            field = field.trim();
                            if (!field.isEmpty()) {
                                if ("ecm:tag".equals(field)) {
                                    TagService tagService = Framework.getService(TagService.class);
                                    List<Tag> tags = tagService.getDocumentTags(session, doc.getId(), null);
                                    StringBuilder sb = new StringBuilder();
                                    for (Iterator<Tag> it = tags.iterator(); it.hasNext(); ) {
                                        Tag tag = it.next();
                                        sb.append(tag.getLabel());
                                        if (it.hasNext()) {
                                            sb.append(", ");
                                        }
                                    }
                                    item.put(field, sb.toString());
                                } else {
                                    try {
                                        Property prop = doc.getProperty(field);
                                        if (prop.isList()) {
                                            item.put(field, doc.getPropertyValue(field));
                                        } else {
                                            LOG.warn("Property " + field + " is not a list");
                                        }
                                    } catch (PropertyNotFoundException e) {
                                        LOG.trace("Ignore document property " + field + " for " + doc.getId());
                                    }
                                }
                            }
                        }
                    }
                } catch (NuxeoException e) {
                    LOG.trace("Document is not found into ResultSet " + item.get("ecm:uuid"));
                }
            } else {
                // Checking ecm:uuid to get list information
                return;
            }
        }
    }

    /**
     * Has field list.
     *
     * @return
     */
    private boolean hasFieldList() {
        try {
            if (fieldList == null) {
                return false;
            }
            return fieldList.get(0) != null && fieldList.get(0).length() > 0;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    /**
     * Has field complex.
     *
     * @return
     */
    private boolean hasFieldComplex() {
        try {
            if (fieldComplex == null) {
                return false;
            }
            return fieldComplex.get(0) != null && fieldComplex.get(0).length() > 0;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }
}