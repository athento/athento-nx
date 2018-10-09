package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.stats.AthentoStatsService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.runtime.api.Framework;

import java.security.Principal;

/**
 * Reindex stat.
 */
@Operation(id = ReindexStatsOperation.ID, category = "Athento", label = "Reindex stat", description = "Reindex stat")
public class ReindexStatsOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(ReindexStatsOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.ReindexStat";

    /** Context. */
    @Context
    protected OperationContext ctx;

    @Param(name = "statName")
    protected String statName;

    @Param(name = "refresh", required = false)
    protected boolean refresh = true;

    @Param(name = "forceAll", required = false)
    protected boolean forceAll = false;

    @Param(name = "batchSize", required = false)
    protected int batchSize = 20;

    /** Run. */
    @OperationMethod
    public String run() throws Exception {
        AthentoStatsService service = Framework.getService(AthentoStatsService.class);
        service.reindexStat(ctx.getCoreSession(), statName, forceAll, batchSize, refresh);
        return "OK";
    }


}
