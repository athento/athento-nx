package org.athento.nuxeo.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

import java.util.List;

/**
 * Remove last blob.
 */
@Operation(id = RemoveLastDocumentBlob.ID, category = Constants.CAT_BLOB, label = "Remove last file", description = "Remove last blob", aliases = { "Blob.RemoveLast" })
public class RemoveLastDocumentBlob {

    public static final String ID = "Blob.RemoveLastBlob";

    @Context
    protected CoreSession session;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        List files = (List) doc.getPropertyValue("files:files");
        if (files.size() > 0) {
            DocumentHelper.removeProperty(doc, "files/" + (files.size() - 1) + "/file");
            doc = session.saveDocument(doc);
        }
        return doc;
    }

}
