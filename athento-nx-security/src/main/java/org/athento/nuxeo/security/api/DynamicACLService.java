package org.athento.nuxeo.security.api;


import org.athento.nuxeo.security.api.descriptor.DynamicACLDescriptor;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;

import java.io.Serializable;
import java.util.List;

/**
 * Dynamic ACL service.
 */
public interface DynamicACLService extends Serializable {

    /**
     * Get Dynamic ACLs for a document.
     *
     * @param doc
     * @return
     */
    List<DynamicACLDescriptor> getDynamicACLsForDoctype(DocumentModel doc);

    /**
     * Get Dynamic ACLs by facet.
     *
     * @param facet
     * @return
     */
    List<DynamicACLDescriptor> getDynamicACLsForFacet(String facet);

    /**
     * Get ACEs for a document.
     *
     * @param dynamicACL
     * @param doc
     * @return
     */
    List<ACE> getACEsForDocument(DynamicACLDescriptor dynamicACL, DocumentModel doc);

    /**
     * Check if a doctype is ignored for Dynamic ACLs.
     *
     * @param doctype
     * @return
     */
    boolean isIgnoredDoctype(String doctype);

}
