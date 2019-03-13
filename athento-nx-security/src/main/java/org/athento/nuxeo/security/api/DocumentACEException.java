package org.athento.nuxeo.security.api;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Document ACE exception.
 *
 * @author <a href="vs@athento.com">Victor Sanchez</a>
 */
public class DocumentACEException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public DocumentACEException(String message) {
        super(message);
    }
}