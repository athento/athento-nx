package org.athento.nuxeo.query;

import org.nuxeo.ecm.core.api.SortInfo;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Query context.
 */
public class QueryContext extends HashMap<String, Object> {

    private ArrayList<String> queries;
    private Map<String, Map<String, Map<String, Serializable>>> results;
    private int currentQueryIndex = -1;
    private int limit;
    private int offset;
    private List<SortInfo> sortInfo;

    Object result = new Object();

    /**
     * Constructor.
     *
     * @param queries
     * @param offset
     * @param limit
     */
    public QueryContext(ArrayList<String> queries, int offset, int limit, List<SortInfo> sortInfo) {
        this.queries = queries;
        if (!this.queries.isEmpty()) {
            this.currentQueryIndex = 0;
        }
        this.limit = limit;
        this.offset = offset;
        this.sortInfo = sortInfo;
        this.results = new HashMap<>();
    }

    public String getCompleteQuery() {
        return String.join("|", this.queries);
    }

    public String getQuery() {
        if (currentQueryIndex > queries.size() - 1) {
            return null;
        }
        String query = queries.get(currentQueryIndex).trim();
        if (query.contains("${")) {
            try {
                query = parseQuery();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.currentQueryIndex++;
        return query;
    }

    private String parseQuery() throws IOException {
        return QueryUtils.expandParams(queries.get(currentQueryIndex), this);
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public List<SortInfo> getSortInfo() {
        return sortInfo;
    }

    /**
     * Get current context result.
     *
     * @return
     */
    public Object getResult() {
        return result;
    }

    /**
     * Is query available.
     *
     * @return
     */
    public boolean isLastQuery() {
        return currentQueryIndex == queries.size() - 1;
    }

    /**
     * Get query results.
     *
     * @return
     */
    public Map<String, Map<String, Map<String, Serializable>>> getSubqueryResults() {
        return results;
    }

    /**
     * Get subquery result.
     *
     * @param query
     * @return
     */
    public Map<String, Map<String, Serializable>> getSubqueryResult(String query) {
        return results.get(query);
    }
}
