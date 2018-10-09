package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.stats.AthentoStatsService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.runtime.api.Framework;

/**
 * Calculate stat.
 */
@Operation(id = CalculateStatOperation.ID, category = "Athento", label = "Calculate stat", description = "Calculate stat")
public class CalculateStatOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(CalculateStatOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.CalculateStat";

    /** Context. */
    @Context
    protected OperationContext ctx;

    @Param(name = "stat")
    protected String stat;

    @Param(name = "statName")
    protected String statName;

    @Param(name = "path")
    protected String path = "/";

    /** Run. */
    @OperationMethod
    public String run() throws Exception {
        AthentoStatsService service = Framework.getService(AthentoStatsService.class);
        service.startCalculation(ctx.getCoreSession(), stat, statName, path);
        return "OK";
    }
}
