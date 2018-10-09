package org.athento.nuxeo.security.util;

import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import java.util.Map;

public final class MvelTemplate {

    protected transient volatile CompiledTemplate compiled;

    protected final String expression;

    public MvelTemplate(String expression) {
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
            compiled = TemplateCompiler.compileTemplate(this.expression);
        }
        return TemplateRuntime.execute(compiled, params);
    }

}
