package org.athento.nuxeo.module;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.operations.utils.AthentoOperationsHelper;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.server.jaxrs.RestOperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Entries endpoint.
 */
@WebObject(type = "entries")
public class EntriesObject extends AbstractResource<ResourceTypeImpl> {

    public static final String PATH = "query";

    public static final String NXQL = "NXQL";

    public static final String QUERY = "query";

    public static final String QUERY_ID = "queryId";

    public static final String PAGE_SIZE = "pageSize";

    public static final String CURRENT_PAGE_INDEX = "currentPageIndex";

    public static final String MAX_RESULTS = "maxResults";

    public static final String SORT_BY = "sortBy";

    public static final String SORT_ORDER = "sortOrder";

    public static final String FIELD_LIST = "fieldList";

    private static final Log LOG = LogFactory.getLog(EntriesObject.class);

    protected EnumMap<QueryParams, String> queryParametersMap;

    protected EnumMap<LangParams, String> langPathMap;

    protected PageProviderService pageProviderService;

    @Override
    public void initialize(Object... args) {
        pageProviderService = Framework.getLocalService(PageProviderService.class);
        // Query Enum Parameters Map
        queryParametersMap = new EnumMap<>(QueryParams.class);
        queryParametersMap.put(QueryParams.PAGE_SIZE, PAGE_SIZE);
        queryParametersMap.put(QueryParams.CURRENT_PAGE_INDEX, CURRENT_PAGE_INDEX);
        queryParametersMap.put(QueryParams.MAX_RESULTS, MAX_RESULTS);
        queryParametersMap.put(QueryParams.SORT_BY, SORT_BY);
        queryParametersMap.put(QueryParams.SORT_ORDER, SORT_ORDER);
        queryParametersMap.put(QueryParams.QUERY, QUERY);
        queryParametersMap.put(QueryParams.FIELDLIST, FIELD_LIST);
        // Lang Path Enum Map
        langPathMap = new EnumMap<>(LangParams.class);
        langPathMap.put(LangParams.NXQL, NXQL);
    }

    @SuppressWarnings("unchecked")
    protected Blob getQuery(UriInfo uriInfo, String langOrProviderName) throws RestOperationException {
        // Fetching all parameters
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        // Look if provider name is given
        String providerName = null;
        if (!langPathMap.containsValue(langOrProviderName)) {
            providerName = langOrProviderName;
        }
        String query = queryParams.getFirst(QUERY);
        String queryId = queryParams.getFirst(QUERY_ID);
        String pageSize = queryParams.getFirst(PAGE_SIZE);
        String currentPageIndex = queryParams.getFirst(CURRENT_PAGE_INDEX);
        String maxResults = queryParams.getFirst(MAX_RESULTS);
        String sortBy = queryParams.getFirst(SORT_BY);
        String sortOrder = queryParams.getFirst(SORT_ORDER);
        String fieldList = queryParams.getFirst(FIELD_LIST);

        if (query == null && StringUtils.isBlank(providerName)) {
            query = "SELECT ecm:uuid from Document";
        }

        // Call to Athento.DocumentResultSet for query
        String OP = "Athento.DocumentFindEntries";
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        if (queryId != null) {
            params.put("queryId", queryId);
        }
        if (currentPageIndex != null) {
            params.put("page", currentPageIndex);
        }
        if (pageSize != null) {
            params.put("pageSize", pageSize);
        }
        if (maxResults != null) {
            params.put("maxResults", maxResults);
        }
        if (sortBy != null) {
            params.put("sortBy", sortBy);
        }
        if (sortOrder != null) {
            params.put("sortOrder", sortOrder);
        }
        if (fieldList != null) {
            params.put("fieldList", fieldList);
        }
        try {
            return (Blob) AthentoOperationsHelper.runOperation(OP, null, params, getContext().getCoreSession());
        } catch (OperationException e) {
            throw new RestOperationException(e);
        }
    }

    /**
     * Perform query to get entries on the repository. By default in NXQL.
     *
     * @param uriInfo Query parameters
     * @return entries
     */
    @GET
    public Object doQuery(@Context UriInfo uriInfo) throws RestOperationException {
        return getQuery(uriInfo, NXQL);
    }

    public enum QueryParams {
        PAGE_SIZE, CURRENT_PAGE_INDEX, MAX_RESULTS, SORT_BY, SORT_ORDER, ORDERED_PARAMS, QUERY, QUERY_ID, FIELDLIST
    }

    public enum LangParams {
        NXQL
    }

}
