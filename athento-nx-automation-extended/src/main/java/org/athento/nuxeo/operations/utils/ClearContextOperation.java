package org.athento.nuxeo.operations.utils;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;

import java.io.IOException;

/**
 * Created by victorsanchez on 19/7/16.
 */
@Operation(id = ClearContextOperation.ID, category = "Athento", label = "Clear operation context", description = "Clear context of automation chain")
public class ClearContextOperation {

    public static final String ID = "Athento.Clear";

    @Context
    protected OperationContext ctx;

    /**
     * Run operation.
     *
     * @param obj
     * @return
     */
    @OperationMethod
    public void run(Object obj) throws IOException {
       ctx.clear();
    }

    /**
     * Run operation.
     *
     * @return
     */
    @OperationMethod
    public void run() throws IOException {
        ctx.clear();
    }


}
