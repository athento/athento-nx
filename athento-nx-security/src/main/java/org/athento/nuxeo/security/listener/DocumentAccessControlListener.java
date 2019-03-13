package org.athento.nuxeo.security.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.security.api.DocumentACEException;
import org.athento.nuxeo.security.api.DocumentACEResult;
import org.athento.nuxeo.security.api.DocumentACEService;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.security.Principal;

/**
 * Document ACE to manage access to schemas or metadatas.
 */
public class DocumentAccessControlListener implements EventListener {

    /** Log. */
    private static Log LOG = LogFactory.getLog(DocumentAccessControlListener.class);
    
    /**
     * Handle event.
     * 
     * @param event document save event
     */
    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();
            Principal principal = ctx.getPrincipal();
            if (LOG.isInfoEnabled()) {
                LOG.info("Document ACEs for doc: " + doc.getId());
            }
            DocumentACEService documentACEService = Framework.getService(DocumentACEService.class);
            if (documentACEService == null) {
                return;
            }
            DocumentACEResult result = null;
            try {
                result = documentACEService.checkDocumentACEs(doc, principal);
            } catch (Exception e) {
                LOG.error("Unable to check document ACEs", e);
            }
            if (result != null && result.hasError()) {
                if (FacesContext.getCurrentInstance() != null) {
                    String message = ComponentUtils.translate(FacesContext.getCurrentInstance(),
                            "error.notallowed");
                    FacesMessage fm = FacesMessages.createFacesMessage(FacesMessage.SEVERITY_ERROR, message);
                    FacesMessages.instance().add(fm);
                }
                TransactionHelper.commitOrRollbackTransaction();
                throw new DocumentACEException("Unauthorized: " + result.getErrors());
            }
        }
    }

}