package org.athento.nuxeo.security.api;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Mimetype exception.
 *
 * @author <a href="vs@athento.com">Victor Sanchez</a>
 */
public class MimetypeException extends
        NuxeoException {

    private static final long serialVersionUID = 1L;

    public MimetypeException(String message) {
        super(message);
    }
}
