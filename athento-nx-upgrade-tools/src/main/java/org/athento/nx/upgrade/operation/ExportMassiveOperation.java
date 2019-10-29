package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.worker.ExportMassiveCSVWorker;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Export massive operation. Using a document as origin to export.
 * */
@Operation(id = ExportMassiveOperation.ID, category = "Athento", label = "Export CSV masive", description = "Export documents from path with references to binaries directory")
public class ExportMassiveOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(ExportMassiveOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.ExportMassive";

    /** Context. */
    @Context
    protected OperationContext ctx;

    @Param(name = "doctype")
    protected String doctype;

    @Param(name = "destinyPath")
    protected String destinyPath;

    @Param(name = "metadatas")
    protected StringList metadatas;

    @Param(name="download", required = false)
    protected boolean download = false;

    @Param(name="downloadPath", required = false)
    protected String downloadPath;

    /** Run. */
    @OperationMethod
    public String run(DocumentModel doc) {
        LOG.info("Starting operation CSV Massive export...");
        ExportMassiveCSVWorker worker = new ExportMassiveCSVWorker(doc, doctype,
                metadatas, destinyPath);
        if (download) {
            worker.setDownloadPath(downloadPath);
        }
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(worker, WorkManager.Scheduling.IF_NOT_RUNNING_OR_SCHEDULED, true);
        return "OK";
    }


}
