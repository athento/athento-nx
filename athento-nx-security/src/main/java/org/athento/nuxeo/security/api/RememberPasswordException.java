package org.athento.nuxeo.security.api;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Remember password exception.
 *
 * @author <a href="vs@athento.com">Victor Sanchez</a>
 */
public class RememberPasswordException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public RememberPasswordException(String message) {
        super(message);
    }

}
