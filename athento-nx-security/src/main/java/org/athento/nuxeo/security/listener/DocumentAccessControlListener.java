package org.athento.nuxeo.security.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.security.api.DocumentACEResult;
import org.athento.nuxeo.security.api.DocumentACEService;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

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
            if (LOG.isDebugEnabled()) {
                LOG.debug("Document ACEs for doc: " + doc.getId());
            }
            DocumentACEService documentACEService = Framework.getService(DocumentACEService.class);
            if (documentACEService == null) {
                return;
            }
            try {
                DocumentACEResult result = documentACEService.checkDocumentACEs(doc, principal);
                if (result.hasError()) {
                    throw new NuxeoException("Unauthorized: " + result.getErrors());
                }
            } catch (Exception e) {
                LOG.error("Unable to check DocumentACEs for document.", e);
            }
        }
    }

}