package org.athento.nuxeo.wf.api;

import org.athento.nuxeo.wf.exception.UpgradeWorkflowException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;

/**
 * Athento routing service interface.
 */
public interface AthentoRoutingService {

    /**
     * Upgrade route.
     *
     * @param route
     * @param doc
     * @param session
     * @throws UpgradeWorkflowException
     */
    void upgradeRoute(DocumentRoute route, DocumentModel doc, CoreSession session) throws UpgradeWorkflowException;

    /**
     * Get document route.
     *
     * @param doc
     * @param processId
     * @param session
     * @return
     */
    DocumentRoute getDocumentRoute(DocumentModel doc, String processId, CoreSession session);

}
