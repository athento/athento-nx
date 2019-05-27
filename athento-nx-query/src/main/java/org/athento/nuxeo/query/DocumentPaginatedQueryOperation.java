package org.athento.nuxeo.query;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.query.core.DocumentModelListPageProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 6.0 Document query operation to perform queries on the repository against Elasticsearch.
 */
@Operation(id = org.athento.nuxeo.query.DocumentPaginatedQueryOperation.ID, category = Constants.CAT_FETCH, label = "ElasticQuery", description = "Perform a query on the repository against Elasticsearch. "
        + "The document list returned will become the input for the next " + "operation.", since = "6.0")
public class DocumentPaginatedQueryOperation {

    public static final String ID = "Document.ElasticQuery";

    public static final String QUERY_SEPARATOR = "|";

    public static final String DESC = "DESC";

    public static final String ASC = "ASC";

    @Context
    protected CoreSession session;

    @Param(name = "query", required = true, description = "The query to perform.")
    protected String query;

    @Param(name = "offset", required = false, description = "Offset.")
    protected Integer offset;

    @Param(name = "limit", required = false, description = "Entries limit size.")
    protected Integer limit;

    @Param(name = "sortBy", required = false, description = "Sort by properties (separated by comma)")
    protected String sortBy;

    @Param(name = "sortOrder", required = false, description = "Sort order, " + "ASC or DESC", widget = Constants.W_OPTION, values = {
            ASC, DESC })
    protected String sortOrder;

    @Param(name = "removeAccents", required = false)
    protected boolean removeAccents = true;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public DocumentModelList run() throws IOException {

        // Target offset
        int targetOffset = 0;
        if (offset != null) {
            targetOffset = offset;
        }
        // Target limit
        int targetLimit = -1;
        if (limit != null) {
            targetLimit = limit;
        }

        // Sort Info Management
        List<SortInfo> sortInfoList = new ArrayList<>();
        if (!StringUtils.isBlank(sortBy)) {
            String[] sorts = sortBy.split(",");
            String[] orders = null;
            if (!StringUtils.isBlank(sortOrder)) {
                orders = sortOrder.split(",");
            }
            for (int i = 0; i < sorts.length; i++) {
                String sort = sorts[i];
                boolean sortAscending = (orders != null && orders.length > i && "asc".equals(orders[i].toLowerCase()));
                sortInfoList.add(new SortInfo(sort, sortAscending));
            }
        }

        DocumentModelListPageProvider pp;
        List<DocumentModel> result;
        // Prepare default query context
        QueryContext queryCtxt;
        // Manage multiple queries
        if (query.contains(QUERY_SEPARATOR)) {
            ArrayList<String> queries = QueryUtils.extractQueriesFromQuery(query, "\\" + QUERY_SEPARATOR, removeAccents);
            queryCtxt = new QueryContext(queries, targetOffset, targetLimit, sortInfoList);
            // Execute queries
            QueryUtils.executeRecursiveQuery(session, queryCtxt);
            // Set limit
            DocumentModelList list = (DocumentModelList) queryCtxt.getResult();
            if (targetLimit < 0 || targetLimit > list.size() - 1) {
                targetLimit = list.size();
            }
            pp = new DocumentModelListPageProvider(list);
            pp.setCurrentPageOffset(targetOffset);
            pp.setPageSize(targetLimit);
            result = pp.getCurrentPage();
        } else {
            if (removeAccents) {
                query = QueryUtils.stripAccents(query);
            }
            // Execute single query
            result = QueryUtils.executeQuery(session, query, targetOffset, targetLimit, sortInfoList);
        }
        return new DocumentModelListImpl(result);
    }

}
