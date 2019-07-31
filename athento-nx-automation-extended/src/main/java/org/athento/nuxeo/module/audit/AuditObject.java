package org.athento.nuxeo.module.audit;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Audit endpoint.
 */
@WebObject(type = "audit")
public class AuditObject extends AbstractResource<ResourceTypeImpl> {


    @Path("workflow")
    public Object getWorkflowAuditEntries() {
        return newObject("workflowEntries");
    }

    @Path("workflow/{id}")
    public Object getWorkflowAuditEntriesForDocument(@PathParam("id") String id) {
        CoreSession session = getContext().getCoreSession();
        DocumentModel doc = session.getDocument(new IdRef(id));
        return newObject("workflowEntries", doc);
    }


}
