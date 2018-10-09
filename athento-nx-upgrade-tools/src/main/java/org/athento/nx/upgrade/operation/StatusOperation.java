package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.api.SystemInfo;
import org.nuxeo.ecm.admin.SystemInfoManager;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.storage.sql.Session;

import java.security.Principal;

/**
 * Status.
 */
@Operation(id = StatusOperation.ID, category = "Athento", label = "Check status", description = "Check status information")
public class StatusOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(StatusOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.CheckStatus";

    /** Context. */
    @Context
    protected OperationContext ctx;

    /** Run. */
    @OperationMethod
    public SystemInfo run() throws Exception {
        SystemInfo systemInfo = new SystemInfo();
        SystemInfoManager sim = new SystemInfoManager();
        // Only for Administrator users
        Principal principal = ctx.getCoreSession().getPrincipal();
        if (((NuxeoPrincipal) principal).isAdministrator()) {
            systemInfo.setHostInfo(sim.getHostInfo());
        }
        systemInfo.setUptime(sim.getUptime());
        return systemInfo;
    }



}
