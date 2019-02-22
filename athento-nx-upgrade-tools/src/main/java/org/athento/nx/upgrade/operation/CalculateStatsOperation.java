package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.stats.AthentoStatsService;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.runtime.api.Framework;

/**
 * Calculate stats.
 */
@Operation(id = CalculateStatsOperation.ID, category = "Athento", label = "Calculate statistics", description = "Calculate statistics")
public class CalculateStatsOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(CalculateStatsOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.CalculateStats";

    @Param(name = "repository", required = false)
    protected String repository = "default";

    @Param(name = "doctypes", required = false)
    protected StringList doctypes;

    @Param(name = "path", required = false)
    protected String path = "/";

    /** Run. */
    @OperationMethod
    public String run() throws Exception {
        AthentoStatsService service = Framework.getService(AthentoStatsService.class);
        if (doctypes == null) {
            doctypes = new StringList();
        }
        if (!service.isCalculationRunning()) {
            service.startCalculation(repository, doctypes.toArray(new String[0]), path);
            return "Started OK";
        } else {
            return "Waiting OK";
        }
    }


}
