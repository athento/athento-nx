package org.athento.nuxeo.security.listener;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.security.api.DynamicACLService;
import org.athento.nuxeo.security.api.descriptor.DynamicACLDescriptor;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

import java.util.List;

/**
 * Dynamic ACL Control listener. It is used to generate dynamic ACEs for a document when it is created or modified.
 * When a document is modified, dynamic ACLs should have overwrite = true to update ACEs.
 */
public class DynamicACLControlListener implements EventListener {

    /** Log. */
    private static Log LOG = LogFactory.getLog(DynamicACLControlListener.class);

    /** Default ACL. */
    private static final String DEFAULT_ACL_NAME = "local";
    
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
            CoreSession session = ctx.getCoreSession();
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() {
                    DocumentModel doc = docCtx.getSourceDocument();
                    if (doc != null) {
                        DynamicACLService dynamicACLService = Framework.getLocalService(DynamicACLService.class);
                        if (!dynamicACLService.isIgnoredDoctype(doc.getDocumentType().getName())) {
                            List<DynamicACLDescriptor> dynamicACLsForDoctype = dynamicACLService.getDynamicACLsForDoctype(doc);
                            for (DynamicACLDescriptor acl : dynamicACLsForDoctype) {
                                List<ACE> aces = dynamicACLService.getACEsForDocument(acl, doc);
                                if (!aces.isEmpty()) {
                                    setACEs(session, doc, aces, acl);
                                }
                            }
                            for (String facet : doc.getFacets()) {
                                List<DynamicACLDescriptor> dynamicACLsForFacet = dynamicACLService.getDynamicACLsForFacet(facet);
                                for (DynamicACLDescriptor acl : dynamicACLsForFacet) {
                                    List<ACE> aces = dynamicACLService.getACEsForDocument(acl, doc);
                                    if (!aces.isEmpty()) {
                                        setACEs(session, doc, aces, acl);
                                    }
                                }
                            }
                        } else {
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Doctype " + doc.getDocumentType().getName() + " is ignored for DynamicACLs");
                            }
                        }
                    }
                }
            }.runUnrestricted();

        }
    }

    /**
     * Set ACEs.
     *
     * @param session
     * @param doc
     * @param aces
     * @param acl
     */
    private void setACEs(CoreSession session, DocumentModel doc, List<ACE> aces, DynamicACLDescriptor acl) {
        LOG.info("Set ACES for DynamicACLS in " + doc.getName() + ", " + aces   );
        String destinyAcl = DEFAULT_ACL_NAME;
        if (acl.acl != null && !acl.acl.isEmpty()) {
            destinyAcl = acl.acl;
        }
        ACPImpl acp = new ACPImpl();
        ACLImpl aclImpl = new ACLImpl(destinyAcl);
        acp.addACL(aclImpl);
        for (ACE ace : aces) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Creating dynamic ACE for " + ace.getUsername() +
                        " for document " + doc.getName() +
                        " in Dynamic ACL " + acl.name + " for " + destinyAcl);
            }
            ACE aceImpl = new ACE(ace.getUsername(), ace.getPermission(), ace.isGranted());
            aclImpl.add(aceImpl);
        }
        if (!aces.isEmpty()) {
            if (acl.blockInheritance) {
                boolean permissionChanged = acp.blockInheritance(destinyAcl, session.getPrincipal().getName());
                if (permissionChanged) {
                    session.setACP(doc.getRef(), acp, acl.overwrite);
                }
            } else {
                session.setACP(doc.getRef(), acp, acl.overwrite);
            }
        }
    }

}