package org.athento.nuxeo.query;

import org.nuxeo.ecm.core.api.SortInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Query context.
 */
public class QueryContext extends HashMap<String, Object> {

    private ArrayList<String> queries;
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
    }

    public String getQuery() throws IOException {
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
}
