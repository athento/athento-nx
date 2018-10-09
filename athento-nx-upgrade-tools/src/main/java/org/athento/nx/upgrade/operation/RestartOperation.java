package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.api.AddonInfo;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.ecm.admin.NuxeoCtlManager;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * Restart server.
 */
@Operation(id = RestartOperation.ID, category = "Athento", label = "Restart server", description = "Restart the server")
public class RestartOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(RestartOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.Restart";

    /** Context. */
    @Context
    protected OperationContext ctx;

    /** Run. */
    @OperationMethod
    public void run() throws Exception {
        new NuxeoCtlManager().restartServer();
    }

}
