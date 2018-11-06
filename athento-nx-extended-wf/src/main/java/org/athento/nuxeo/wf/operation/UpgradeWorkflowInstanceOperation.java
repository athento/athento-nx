package org.athento.nuxeo.wf.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.wf.api.AthentoRoutingService;
import org.athento.nuxeo.wf.exception.UpgradeWorkflowException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;

/**
 * Operation to upgrade a workflow instance into document.
 */
@Operation(id = UpgradeWorkflowInstanceOperation.ID, category = "Athento", label = "Upgrade a workflow instance", description = "Upgrade a workflow instance into document.")
public class UpgradeWorkflowInstanceOperation {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(UpgradeWorkflowInstanceOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.SynchronizeWorkflowInstance";

    /** Operation context. */
    @Context
    protected OperationContext ctx;

    /** Core session. */
    @Context
    protected CoreSession session;

    @Context
    protected AthentoRoutingService athentoRouting;

    /** Workflow model id to upgrade. */
    @Param(name = "modelId")
    protected String modelId;

    /**
     * Run operation for a document.
     *
     * @param doc is the document
     * @return the document
     */
    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws UpgradeWorkflowException {
        // Get a workflow instance for document given the modelId
        DocumentRoute currentRoute = athentoRouting.getDocumentRoute(doc, modelId, session);
        if (currentRoute == null) {
            throw new UpgradeWorkflowException("Document route with modelId "
                    + modelId + " is not found in document");
        }
        // Synchronize route
        athentoRouting.upgradeRoute(currentRoute, doc, session);
        return doc;
    }



}
