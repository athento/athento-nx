package org.athento.nuxeo.operations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.operations.security.AbstractAthentoOperation;
import org.nuxeo.ecm.automation.OperationContext;
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
import org.nuxeo.ecm.platform.relations.api.*;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Athento Document Copy with relations.
 *
 * @author athento
 */
@Operation(id = AthentoDocumentCopyOperation.ID, category = "Athento", label = "Athento Document Copy", description = "Copy a document with relations")
public class AthentoDocumentCopyOperation extends AbstractAthentoOperation {

    private static final Log LOG = LogFactory
            .getLog(AthentoDocumentCopyOperation.class);


    public static final String ID = "Athento.DocumentCopy";

    public static final String REFERENCE_PREDICATE = "http://purl.org/dc/terms/References";

    @Context
    protected CoreSession session;

    /** Operation context. */
    @Context
    protected OperationContext ctx;

    @Context
    protected RelationManager relationManager;

    @Context
    protected DocumentRelationManager docRelationManager;

    @Param(name = "target")
    protected DocumentRef target; // the path or the ID

    @Param(name = "name", required = false)
    protected String name;

    @Param(name = "copyRelations", required = false)
    protected boolean relations = false;

    /**
     * Copy a document given its document ref.
     *
     * @param ref
     * @return
     */
    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef ref) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Copying doc by ref...");
        }
        String n = name;
        DocumentModel doc = session.getDocument(ref);
        if (name == null || name.length() == 0) {
            n = doc.getName();
        }
        DocumentModel copy = session.copy(ref, target, n);
        if (relations) {
            DocumentModelList outgoingDocs = getDocuments(getDocumentResource(doc), new ResourceImpl(REFERENCE_PREDICATE), true);
            for (DocumentModel outgoingDoc : outgoingDocs) {
                docRelationManager.addRelation(session, copy, outgoingDoc, REFERENCE_PREDICATE, false);
            }
            DocumentModelList ingoingDocs = getDocuments(getDocumentResource(doc), new ResourceImpl(REFERENCE_PREDICATE), false);
            for (DocumentModel ingoingDoc : ingoingDocs) {
                docRelationManager.addRelation(session, copy, ingoingDoc, REFERENCE_PREDICATE, true);
            }
        }
        return copy;
    }

    /**
     * Copy a document.
     *
     * @param doc
     * @return
     */
    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Copying a doc...");
        }
        return run(doc.getRef());
    }

    /**
     * Copy a list of documents.
     *
     * @param docs
     * @return
     */
    @OperationMethod(collector = DocumentModelListCollector.class)
    public DocumentModelList run(DocumentModelList docs) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Copying multiple docs...");
        }
        DocumentModelList copies = new DocumentModelListImpl();
        for (DocumentModel doc : docs) {
            DocumentModel copy = run(doc.getRef());
            copies.add(copy);
        }
        return copies;
    }

    protected QNameResource getDocumentResource(DocumentModel document) {
        return (QNameResource) relationManager.getResource(RelationConstants.DOCUMENT_NAMESPACE, document, null);
    }

    protected DocumentModelList getDocuments(QNameResource res, Resource predicate, boolean outgoing) {
        if (outgoing) {
            List<Statement> statements = getOutgoingStatements(res, predicate);
            DocumentModelList docs = new DocumentModelListImpl(statements.size());
            for (Statement st : statements) {
                DocumentModel dm = getDocumentModel(st.getObject());
                if (dm != null) {
                    docs.add(dm);
                }
            }
            return docs;
        } else {
            List<Statement> statements = getIncomingStatements(res, predicate);
            DocumentModelList docs = new DocumentModelListImpl(statements.size());
            for (Statement st : statements) {
                DocumentModel dm = getDocumentModel(st.getSubject());
                if (dm != null) {
                    docs.add(dm);
                }
            }
            return docs;
        }
    }

    protected DocumentModel getDocumentModel(Node node) {
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) node;
            Map<String, Object> context = Collections.singletonMap(
                    ResourceAdapter.CORE_SESSION_CONTEXT_KEY, session);
            Object o = relationManager.getResourceRepresentation(resource.getNamespace(), resource, context);
            if (o instanceof DocumentModel) {
                return (DocumentModel) o;
            }
        }
        return null;
    }

    protected List<Statement> getIncomingStatements(QNameResource res, Resource predicate) {
        return relationManager.getGraphByName(RelationConstants.GRAPH_NAME).getStatements(null, predicate, res);
    }

    protected List<Statement> getOutgoingStatements(QNameResource res, Resource predicate) {
        return relationManager.getGraphByName(RelationConstants.GRAPH_NAME).getStatements(res, predicate, null);
    }


}