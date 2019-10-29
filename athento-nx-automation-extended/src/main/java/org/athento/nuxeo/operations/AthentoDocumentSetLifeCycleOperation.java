package org.athento.nuxeo.operations;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelListCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;


@Operation(id = AthentoDocumentCreateOperation.ID, category = "Athento", label = "Athento Document Change LifeCycle", description = "Set Lifecycle to document or list of documents")
public class AthentoDocumentSetLifeCycleOperation {

    public static final String ID = "Document.FollowLifecycleTransition";

    @Context
    protected CoreSession session;

    @Param(name = "value")
    protected String value;

    @OperationMethod(collector = DocumentModelListCollector.class)
    public DocumentModelList run(DocumentModelList docs) {
        DocumentModelList result = new DocumentModelListImpl();
        for (DocumentModel doc : docs) {
            result.add(run(doc));
        }
        return result;
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        session.followTransition(doc.getRef(), value);
        return session.getDocument(doc.getRef());
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef doc) {
        session.followTransition(doc, value);
        return session.getDocument(doc);
    }

}
