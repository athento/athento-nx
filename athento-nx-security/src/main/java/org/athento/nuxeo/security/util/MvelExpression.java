package org.athento.nuxeo.security.util;

import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.Map;

public final class MvelExpression {

    protected transient volatile Serializable compiled;

    protected final String expression;

    public MvelExpression(String expression) {
        this.expression = expression;
    }

    /**
     * Eval.
     *
     * @param params
     * @return
     */
    public Object eval(Map<String, Object> params) {
        if (compiled == null) {
            compiled = MVEL.compileExpression(this.expression);
        }
        return MVEL.executeExpression(compiled, params);
    }

}
