package org.athento.nuxeo.query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.search.aggregations.Aggregation;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.*;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.elasticsearch.aggregate.AggregateEsBase;
import org.nuxeo.elasticsearch.aggregate.AggregateFactory;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.*;

/**
 * Elasticsearch Query and Fetch Page provider.
 *
 * @since 6.0
 */
public class ElasticSearchQueryAndFetchPageProvider extends AbstractPageProvider<Map<String, Serializable>> {

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String CHECK_QUERY_CACHE_PROPERTY = "checkQueryCache";

    public static final String SEARCH_ON_ALL_REPOSITORIES_PROPERTY = "searchAllRepositories";

    public static final String LANGUAGE_PROPERTY = "language";

    protected static final Log log = LogFactory.getLog(ElasticSearchQueryAndFetchPageProvider.class);

    private static final long serialVersionUID = 1L;

    protected String query;

    protected List<Map<String, Serializable>> currentItems;

    protected HashMap<String, Aggregate<? extends Bucket>> currentAggregates;

    @Override
    public List<Map<String, Serializable>> getCurrentPage() {
        checkQueryCache();
        if (currentItems == null) {
            errorMessage = null;
            error = null;

            if (query == null) {
                buildQuery();
            }
            if (query == null) {
                throw new NuxeoException(String.format("Cannot perform null Elastic query: check provider '%s'",
                        getName()));
            }

            currentItems = new ArrayList<Map<String, Serializable>>();

            Map<String, Serializable> props = getProperties();
            CoreSession coreSession = (CoreSession) props.get(CORE_SESSION_PROPERTY);
            if (coreSession == null) {
                throw new NuxeoException("cannot find core session");
            }

            IterableQueryResult result = null;
            try {

                long minMaxPageSize = getMinMaxPageSize();

                long offset = getCurrentPageOffset();
                if (log.isInfoEnabled()) {
                    log.info(String.format("Performing query for provider '%s': '%s' with pageSize=%s, offset=%s",
                            getName(), query, Long.valueOf(minMaxPageSize), Long.valueOf(offset)));
                }
                // Build and execute the ES query
                ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
                NxQueryBuilder nxQuery = new NxQueryBuilder(getCoreSession()).nxql(query).offset(
                        (int) getCurrentPageOffset()).limit((int) getMinMaxPageSize()).addAggregates(buildAggregates());
                if (searchOnAllRepositories()) {
                    nxQuery.searchOnAllRepositories();
                }
                EsResult ret = ess.queryAndAggregate(nxQuery);
                if (!nxQuery.returnsDocuments()) {
                    result = ret.getRows();
                }

                if (result == null) {
                    return this.currentItems;
                }

                long resultsCount = result.size();
                setResultsCount(resultsCount);

                if (log.isDebugEnabled()) {
                    log.debug("Total result " + resultsCount);
                }

                /*IGNORE to check for a BUG? if (offset < resultsCount) {
                    result.skipTo(offset);
                }*/

                Iterator<Map<String, Serializable>> it = result.iterator();

                int pos = 0;
                while (it.hasNext() && (maxPageSize == 0 || pos <= minMaxPageSize + offset)) {
                    if (pos > offset) {
                        Map<String, Serializable> item = it.next();

                        currentItems.add(item);
                    }
                    pos++;
                }

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Performed query for provider '%s': got %s hits", getName(),
                            Long.valueOf(resultsCount)));
                }

                long pageSize = getPageSize();
                if (pageSize != 0) {
                    if (offset != 0 && currentItems.size() == 0) {
                        if (resultsCount == 0) {
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Current page %s is not the first one but "
                                                + "shows no result and there are " + "no results => rewind to first page",
                                        Long.valueOf(getCurrentPageIndex())));
                            }
                            firstPage();
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Current page %s is not the first one but "
                                                + "shows no result and there are " + "%s results => fetch last page",
                                        Long.valueOf(getCurrentPageIndex()), Long.valueOf(resultsCount)));
                            }
                            lastPage();
                        }
                        getCurrentPage();
                    }
                }

            } catch (NuxeoException e) {
                errorMessage = e.getMessage();
                error = e;
                log.warn(e.getMessage(), e);
            } finally {
                if (result != null) {
                    result.close();
                }
            }
        }
        return currentItems;
    }

    protected CoreSession getCoreSession() {
        Map<String, Serializable> props = getProperties();
        CoreSession coreSession = (CoreSession) props.get(CORE_SESSION_PROPERTY);
        if (coreSession == null) {
            throw new NuxeoException("cannot find core session");
        }
        return coreSession;
    }

    private List<AggregateEsBase<? extends Aggregation, ? extends Bucket>> buildAggregates() {
        List<AggregateDefinition> aggregateDefinitions = getAggregateDefinitions();
        ArrayList<AggregateEsBase<? extends Aggregation, ? extends Bucket>> ret;
        if (aggregateDefinitions != null) {
            ret = new ArrayList<>(
                    getAggregateDefinitions().size());
            for (AggregateDefinition def : getAggregateDefinitions()) {
                ret.add(AggregateFactory.create(def, getSearchDocumentModel()));
            }
        } else {
            ret = new ArrayList<>(0);
        }
        return ret;
    }

    protected boolean searchOnAllRepositories() {
        String value = (String) getProperties().get(SEARCH_ON_ALL_REPOSITORIES_PROPERTY);
        if (value == null) {
            return false;
        }
        return Boolean.valueOf(value);
    }

    protected void checkQueryCache() {
        if (getBooleanProperty(CHECK_QUERY_CACHE_PROPERTY, false)) {
            buildQuery();
        }
    }

    /**
     * Build query.
     */
    protected void buildQuery() {
        PageProviderDefinition def = getDefinition();
        String originalQuery = def.getPattern();

        SortInfo[] sortArray = null;
        if (sortInfos != null) {
            sortArray = sortInfos.toArray(new SortInfo[] {});
        }
        String newQuery = NXQLQueryBuilder.getQuery(originalQuery, getParameters(),
                def.getQuotePatternParameters(), def.getEscapePatternParameters(), getSearchDocumentModel(),
                sortArray);

        if (query != null && newQuery != null && !newQuery.equals(query)) {
            refresh();
        }
        query = newQuery;
    }

    @Override
    public boolean hasAggregateSupport() {
        return true;
    }

    @Override
    public Map<String, Aggregate<? extends Bucket>> getAggregates() {
        getCurrentPage();
        return currentAggregates;
    }

}
