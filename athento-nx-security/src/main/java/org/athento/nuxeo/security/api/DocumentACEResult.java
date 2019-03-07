package org.athento.nuxeo.security.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Document ACE Result.
 */
public class DocumentACEResult {

    List<String> errors = new ArrayList<>();

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    /**
     * Check if has errors.
     *
     * @return
     */
    public boolean hasError() {
        return !errors.isEmpty();
    }

}
