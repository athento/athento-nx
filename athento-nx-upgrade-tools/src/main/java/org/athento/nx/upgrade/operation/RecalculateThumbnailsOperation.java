package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.impl.AbstractProperty;

/**
 * Recalculate thumbnails.
 */
@Operation(id = RecalculateThumbnailsOperation.ID, category = "Athento", label = "Recalculate thumbnails", description = "Recalculate thumbnails")
public class RecalculateThumbnailsOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(RecalculateThumbnailsOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.RecalculateThumbnails";

    /** Session. */
    @Context
    protected CoreSession session;

    /** Run. */
    @OperationMethod
    public DocumentModel run(DocumentModel folder) {
        DocumentModelList children = session.getChildren(folder.getRef());
        for (DocumentModel doc : children) {
            if (doc.hasSchema("file") && doc.hasFacet("Thumbnail")) {
                AbstractProperty content = (AbstractProperty) doc.getProperty("file:content");
                content.setIsModified();
                session.saveDocument(doc);
            }
        }
        return folder;
    }


}
