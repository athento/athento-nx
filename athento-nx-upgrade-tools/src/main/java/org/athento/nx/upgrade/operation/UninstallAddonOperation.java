package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.api.AddonInfo;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.model.PackageDefinition;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.ecm.admin.NuxeoCtlManager;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Uninstall addon.
 */
@Operation(id = UninstallAddonOperation.ID, category = "Athento", label = "Uninstall Addon", description = "Uninstall an addon")
public class UninstallAddonOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(UninstallAddonOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.UninstallAddon";

    /** Context. */
    @Context
    protected OperationContext ctx;

    @Context
    protected PackageUpdateService pus;

    @Param(name = "addonName", required = true)
    protected String addonName;

    @Param(name = "restart", required = false)
    protected boolean restart = false;

    /** Run. */
    @OperationMethod
    public String run() throws Exception {
        LocalPackage pkg = pus.getPackage(addonName);
        if (pkg == null) {
            throw new PackageException("Package " + addonName + " is not found");
        }
        uninstallPackage(pkg);
        return pkg.getId();
    }

    /**
     * Uninstal package.
     *
     * @param pkg
     */
    private void uninstallPackage(LocalPackage pkg) throws PackageException {
        if (pkg == null) {
            return;
        }
        LOG.info("Uninstalling pkg " + pkg.getId());
        Task uninstallTask = pkg.getUninstallTask();
        Map<String, String> params = new HashMap<>();
        try {
            uninstallTask.run(params);
            // Delete package after uninstall
            pus.removePackage(pkg.getId());
        } catch (Throwable e) {
            LOG.error("Error during un-installation of " + pkg, e);
            uninstallTask.rollback();
            throw new PackageException("Unable to uninstall package: " + e.getMessage());
        }
    }


}
