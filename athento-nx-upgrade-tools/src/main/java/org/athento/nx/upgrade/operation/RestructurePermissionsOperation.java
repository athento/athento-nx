package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.worker.UpgradePermissionWorker;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Re-structure ACLs from 5.x to 6.0.
 */
@Operation(id = RestructurePermissionsOperation.ID, category = "Athento", label = "Restructure ACLs from 5.x to 6.0", description = "Restructure ACLs from 5.x to 6.0")
public class RestructurePermissionsOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(RestructurePermissionsOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.UpgradeACL";

    /** Context. */
    @Context
    protected OperationContext ctx;

    /** Acl name. */
    @Param(name = "aclName", required = false, description = "ACL name to upgrade")
    private String aclName =  "local";

    @Param(name = "onlyFolder", required = false, description = "Update permission only in folder levels")
    private boolean onlyFolder = true;

    @Param(name = "save", required = false, description = "Save permission after restructuration")
    private boolean save = false;

    /** Run. */
    @OperationMethod
    public String run(DocumentModel doc) throws Exception {

        // Start worker
        startUpgradeWorker(doc);

        return "OK";

    }

    /**
     * Start upgrade worker.
     *
     * @param doc) is the root documento to upgrade the acl
     */
    private void startUpgradeWorker(DocumentModel doc) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting upgrade permission worker...");
        }
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        UpgradePermissionWorker worker = new UpgradePermissionWorker(doc, aclName);
        worker.setOnlyFolders(onlyFolder);
        worker.saveAtEnd(save);
        workManager.schedule(worker, WorkManager.Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
    }


}
