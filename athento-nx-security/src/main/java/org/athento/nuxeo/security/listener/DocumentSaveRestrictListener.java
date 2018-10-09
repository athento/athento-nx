package org.athento.nuxeo.security.listener;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.security.api.MimetypeException;
import org.athento.nuxeo.security.util.MimeUtils;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/**
 * Document save with security restriction.
 */
public class DocumentSaveRestrictListener implements EventListener {

    /** Log. */
    private static Log LOG = LogFactory.getLog(DocumentSaveRestrictListener.class);
    
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
            try {
                MimeUtils.checkMimeType(doc);
            } catch (MimetypeException e) {
                LOG.error("Mimetype control exception", e);
                String message = ComponentUtils.translate(FacesContext.getCurrentInstance(),
                        "mimetype.error.notallowed");
                FacesMessage fm = FacesMessages.createFacesMessage(FacesMessage.SEVERITY_ERROR, message);
                FacesMessages.instance().add(fm);
                throw e;
            }
        }
    }

}