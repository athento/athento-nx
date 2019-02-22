package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.api.AddonInfo;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * Get addon information.
 */
@Operation(id = GetAddonOperation.ID, category = "Athento", label = "Get Addon information", description = "Get information of addon")
public class GetAddonOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(GetAddonOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.GetAddon";

    /** Context. */
    @Context
    protected OperationContext ctx;

    @Context
    protected PackageUpdateService pus;

    @Param(name = "addonName", required = true)
    protected String addonName;


    /** Run. */
    @OperationMethod
    public AddonInfo run() throws Exception {
        LocalPackage pkg = pus.getPackage(addonName);
        if (pkg == null) {
            throw new PackageException("Package " + addonName + " is not found");
        }
        AddonInfo addonInfo = new AddonInfo();
        addonInfo.setVersion(pkg.getVersion().toString());
        addonInfo.setName(pkg.getName());
        addonInfo.setStatus(pkg.getPackageState().getLabel());
        addonInfo.setInstalled(pkg.getPackageState().isInstalled());
        return addonInfo;
    }

}
