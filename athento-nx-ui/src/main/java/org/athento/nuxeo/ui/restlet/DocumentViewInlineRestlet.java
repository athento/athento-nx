package org.athento.nuxeo.ui.restlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.ui.util.Utils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseNuxeoRestlet;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationServiceImpl;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants.*;

/**
 * View inline a document content.
 */
@Name("viewinline")
@Scope(ScopeType.EVENT)
public class DocumentViewInlineRestlet extends BaseNuxeoRestlet {

    private static final Log LOG = LogFactory.getLog(DocumentViewInlineRestlet.class);

    private static final long serialVersionUID = -5765462193748634334L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected CoreSession documentManager;

    @Override
    public void handle(Request req, Response res) {

        String repo = (String) req.getAttributes().get("repo");
        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }

        boolean isAuthenticated = false;
        String token = getQueryParamValue(req, "token", "");
        if (token == null || token.isEmpty()) {
            try {
                Framework.login();
                // Get token from user
                navigationContext.setCurrentServerLocation(new RepositoryLocation(
                        repo));
                documentManager = navigationContext.getOrCreateDocumentManager();
                isAuthenticated = true;
            } catch (Exception e) {
                LOG.error("Error for preview pdf inline", e);
                // DO NOTHING
            }
        }

        DocumentModel dm;
        try {
            if (!isAuthenticated) {
                Framework.login();
                navigationContext.setCurrentServerLocation(new RepositoryLocation(
                        repo));
                documentManager = navigationContext.getOrCreateDocumentManager();
            }
            String docid = (String) req.getAttributes().get("docid");
            if (docid != null) {
                dm = documentManager.getDocument(new IdRef(docid));
                // Check if session is authenticate to manage or not the token
                if (token != null && !isAuthenticated) {
                    if (!Utils.validToken(token, dm)) {
                        handleError(res, "Token is invalid.");
                        return;
                    }
                }
            } else {
                handleError(res, "you must specify a valid document IdRef");
                return;
            }
        } catch (LoginException e) {
            handleError(res, e);
            return;
        }

        try {
            final String filename;
            final Blob blob;

            String blobPropertyName = getQueryParamValue(req, BLOB_PROPERTY_NAME, null);
            String filenamePropertyName = getQueryParamValue(req, FILENAME_PROPERTY_NAME, null);
            if (blobPropertyName != null && filenamePropertyName != null) {
                filename = (String) dm.getPropertyValue(filenamePropertyName);
                blob = (Blob) dm.getPropertyValue(blobPropertyName);
            } else {
                String schemaName = getQueryParamValue(req, SCHEMA, DEFAULT_SCHEMA);
                String blobFieldName = getQueryParamValue(req, BLOB_FIELD, DEFAULT_BLOB_FIELD);
                String filenameFieldName = getQueryParamValue(req, FILENAME_FIELD, DEFAULT_FILENAME_FIELD);
                filename = (String) dm.getProperty(schemaName, filenameFieldName);
                blob = (Blob) dm.getProperty(schemaName, blobFieldName);
            }

            final File tempfile = File.createTempFile("athento-inlinerestlet-tmp", "");
            blob.transferTo(tempfile);
            res.setEntity(new OutputRepresentation(null) {
                @Override
                public void write(OutputStream outputStream) throws IOException {
                    FileInputStream instream = new FileInputStream(tempfile);
                    FileUtils.copy(instream, outputStream);
                    instream.close();
                }
            });
            HttpServletResponse response = getHttpResponse(res);
            response.setHeader("Content-Type", blob.getMimeType());
            response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\";");
        } catch (Exception e) {
            handleError(res, e);
        }
    }


}
