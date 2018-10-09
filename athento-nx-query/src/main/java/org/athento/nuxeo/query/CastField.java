package org.athento.nuxeo.query;

import java.io.Serializable;

/**
 * Cast field.
 */
public class CastField {

    protected String field;
    protected Serializable value;
    protected boolean original = false;

    /**
     * Constructor.
     *
     * @param field
     * @param value
     */
    public CastField(String field, Serializable value) {
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }

    public boolean isOriginal() {
        return original;
    }

    public void setOriginal(boolean original) {
        this.original = original;
    }
}
