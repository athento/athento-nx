package org.athento.nuxeo.security.api;


import org.athento.nuxeo.security.api.descriptor.DocumentACEDescriptor;
import org.nuxeo.ecm.core.api.DocumentModel;

import java.io.Serializable;
import java.security.Principal;

/**
 * Document ACE service.
 */
public interface DocumentACEService extends Serializable {

    /**
     * Check document ACEs.
     *
     * @param doc
     * @param principal
     * @return
     */
    DocumentACEResult checkDocumentACEs(DocumentModel doc, Principal principal);

    /**
     * Get document ACEs.
     *
     * @param doctype
     * @return
     */
    DocumentACEDescriptor getDocumentACE(String doctype);


}
