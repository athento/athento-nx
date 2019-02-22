package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * Install addon.
 */
@Operation(id = InstallAddonOperation.ID, category = "Athento", label = "Install Addon", description = "Upload and install an addon")
public class InstallAddonOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(InstallAddonOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.InstallAddon";

    /** Context. */
    @Context
    protected OperationContext ctx;

    @Context
    protected PackageUpdateService pus;

    @Param(name = "restart", required = false)
    protected boolean restart = false;

    @Param(name = "overwrite", required = false)
    protected boolean overwrite = false;

    /** Run. */
    @OperationMethod
    public String run(Blob blob) throws Exception {
        // Create temporal file
        File tmpAddonFile = File.createTempFile("ath-addon-tmp", ".pkg");
        blob.transferTo(tmpAddonFile);
        LocalPackage pkg;
        try {
            PackageDefinition pkgDef = pus.loadPackageFromZip(tmpAddonFile);
            // Check if package exists
            if (packageExists(pkgDef.getId())) {
                if (!overwrite) {
                    return "Package exists";
                }
                // Uninstall package
                uninstallPackage(pkgDef.getId());
            }
            pkg = pus.addPackage(tmpAddonFile);
            if (pkg != null) {
                // Install package
                installPackage(pkg);
            }
        } finally {
            tmpAddonFile.delete();
        }
        if (pkg != null) {
            return pkg.getId();
        } else {
            throw new Exception("Unable to install package.");
        }
    }

    /**
     * Uninstal package.
     *
     * @param pkgId
     */
    private void uninstallPackage(String pkgId) throws PackageException {
        LocalPackage pkg = pus.getPackage(pkgId);
        if (pkg == null) {
            return;
        }
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

    /**
     * Install a package.
     *
     * @param pkg
     */
    private void installPackage(LocalPackage pkg) throws PackageException {
        Task installTask = pkg.getInstallTask();
        Map<String, String> params = new HashMap<>();
        try {
            installTask.run(params);
        } catch (Throwable e) {
            LOG.error("Error during installation of " + pkg, e);
            installTask.rollback();
            throw new PackageException("Unable to install package: " + e.getMessage());
        }
        // restart
        if (restart) {
            new NuxeoCtlManager().restartServer();
        }
    }

    /**
     * Check if package exists.
     *
     * @param pkgId
     * @return
     */
    private boolean packageExists(String pkgId) throws PackageException {
        return pus.getPackage(pkgId) != null;
    }


}
