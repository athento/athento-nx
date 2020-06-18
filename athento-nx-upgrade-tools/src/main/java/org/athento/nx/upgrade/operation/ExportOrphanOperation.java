package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.worker.ExportMassiveCSVWorker;
import org.athento.nx.upgrade.worker.ExportOrphanWorker;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

import java.util.Date;

/**
 * Export orphan documents operation.
 * */
@Operation(id = ExportOrphanOperation.ID, category = "Athento", label = "Export Orphan binaries between dates", description = "Export orphan documents between two dates")
public class ExportOrphanOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(ExportOrphanOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.ExportOrphan";

    /** Context. */
    @Context
    protected OperationContext ctx;

    @Param(name = "startDate")
    protected Date startDate;

    @Param(name = "endDate")
    protected Date endDate;

    @Param(name="repoBinariesPath", required = false)
    protected String repoBinariesPath;

    @Param(name="downloadPath", required = false)
    protected String downloadPath;

    /** Run. */
    @OperationMethod
    public String run(DocumentModel doc) {
        LOG.info("Starting operation Orphan export...");
        ExportOrphanWorker worker = new ExportOrphanWorker(doc, startDate, endDate, downloadPath, repoBinariesPath);
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(worker, WorkManager.Scheduling.IF_NOT_RUNNING_OR_SCHEDULED, true);
        return "OK";
    }


}
