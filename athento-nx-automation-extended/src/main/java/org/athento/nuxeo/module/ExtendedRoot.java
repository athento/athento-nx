package org.athento.nuxeo.module;

import org.nuxeo.ecm.automation.server.jaxrs.RestOperationException;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

/**
 * The root entry for the WebEngine module for Athento.
 *
 * @since 8.10
 */
@Path("/api/athento/v1")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "ExtendedRoot")
public class ExtendedRoot extends ModuleRoot {

    @Path("/entries")
    public Object doEntries() {
        return newObject("entries");
    }

    @Override
    public Object handleError(final WebApplicationException cause) {
        Throwable unWrapException = ExceptionHelper.unwrapException(cause);
        if (unWrapException instanceof RestOperationException) {
            int customHttpStatus = ((RestOperationException) unWrapException)
                    .getStatus();
            return WebException.newException(
                    cause.getMessage(), cause, customHttpStatus);
        }
        return WebException.newException(
                cause.getMessage(), unWrapException);
    }

}