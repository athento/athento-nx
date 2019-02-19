package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.stats.AthentoStatsService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.runtime.api.Framework;

/**
 * Reindex by query.
 */
@Operation(id = ElasticsearchReindexQueryOperation.ID, category = "Athento", label = "Elasticsearch Reindex by query", description = "Reindex by query")
public class ElasticsearchReindexQueryOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(ElasticsearchReindexQueryOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.ReindexQuery";

    public static final String WHERE_CLAUSE = " where ";

    /** Context. */
    @Context
    protected OperationContext ctx;

    @Param(name = "query")
    protected String query;

    /** Run. */
    @OperationMethod
    public void run() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Reindex with query " + query);
        }
        // Control from global query (reindex)
        if (!query.toLowerCase().contains(WHERE_CLAUSE)) {
            LOG.info("Ignore reindex operation with query: " + query);
            return;
        }
        ElasticSearchIndexing esi = Framework.getService(ElasticSearchIndexing.class);
        esi.runReindexingWorker("default", query);
    }


}
