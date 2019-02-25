package org.athento.nuxeo.ui.restlet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.ui.util.Utils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.automation.core.rendering.RenderingService;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.NothingToPreviewException;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseNuxeoRestlet;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * Preview restlet with no auth. Based on PreviewRestlet.java of Nuxeo DM (c).
 */
@Name("athentoPreviewRestlet")
@Scope(ScopeType.EVENT)
public class PreviewRestlet extends BaseNuxeoRestlet {

    private static final Log LOG = LogFactory.getLog(PreviewRestlet.class);

    @In(create = true)
    protected NavigationContext navigationContext;

    protected CoreSession documentManager;

    protected DocumentModel targetDocument;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    protected static final List<String> previewInProcessing = Collections
            .synchronizedList(new ArrayList<String>());

    public void handle(Request req, Response res) {

        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        String xpath = (String) req.getAttributes().get("fieldPath");
        String token = getQueryParamValue(req, "token", "");

        if (token == null) {
            handleError(res, "Token is mandatory for preview");
            return;
        }

        xpath = xpath.replace("-", "/");
        List<String> segments = req.getResourceRef().getSegments();
        StringBuilder sb = new StringBuilder();
        for (int i = 6; i < segments.size(); i++) {
            sb.append(segments.get(i));
            sb.append("/");
        }
        String subPath = sb.substring(0, sb.length() - 1);

        try {
            xpath = URLDecoder.decode(xpath, "UTF-8");
            subPath = URLDecoder.decode(subPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error(e);
        }

        String blobPostProcessingParameter = getQueryParamValue(req,
                "blobPostProcessing", "false");
        boolean blobPostProcessing = Boolean
                .parseBoolean(blobPostProcessingParameter);

        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }
        if (docid == null || repo.equals("*")) {
            handleError(res, "you must specify a documentId");
            return;
        }

        try {
            Framework.login();
            navigationContext.setCurrentServerLocation(new RepositoryLocation(
                    repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            targetDocument = documentManager.getDocument(new IdRef(docid));
        } catch (DocumentNotFoundException e) {
            LOG.error("Unable to get document from session", e);
            handleError(res, e);
            return;
        } catch (LoginException e) {
            LOG.error("Login error in Athento templates", e);
            return;
        }

        if (!ignoreSubpathAccess(subPath, sb.toString())) {
            if (!Utils.validToken(token, targetDocument)) {
                handleError(res, "Token is invalid.");
                return;
            } else {
                LOG.info("Token is valid");
            }
        } else {
            LOG.info("Ignoring subpath for " + subPath + ", " + req.getResourceRef());
        }

        List<Blob> previewBlobs;
        try {
            previewBlobs = initCachedBlob(res, xpath, blobPostProcessing);
        } catch (Exception e) {
            handleError(res, "unable to get templates");
            return;
        }
        if (previewBlobs == null || previewBlobs.isEmpty()) {
            // response was already handled by initCachedBlob
            return;
        }
        HttpServletResponse response = getHttpResponse(res);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");

        try {
            if (subPath == null || "".equals(subPath)) {
                handlePreview(res, previewBlobs.get(0), "text/html");
                return;
            } else {
                for (Blob blob : previewBlobs) {
                    if (subPath.equals(blob.getFilename())) {
                        handlePreview(res, blob, blob.getMimeType());
                        return;
                    }

                }
            }
        } catch (IOException e) {
            handleError(res, e);
        }
    }

    /**
     * Check ignore subpath access.
     *
     * @param subPath
     * @return
     */
    private boolean ignoreSubpathAccess(String subPath, String url) {
        return subPath != null && !subPath.isEmpty() && url != null && !url.isEmpty()
                && (!subPath.toLowerCase().endsWith(".png")
                || !subPath.toLowerCase().endsWith(".jpg")
                || !subPath.toLowerCase().endsWith(".jpeg")
                || !url.toLowerCase().contains("gettiles"));
    }

    private List<Blob> initCachedBlob(Response res, String xpath,
                                      boolean blobPostProcessing) {

        HtmlPreviewAdapter preview = null; // getFromCache(targetDocument,
        // xpath);

        // if (templates == null) {
        preview = targetDocument.getAdapter(HtmlPreviewAdapter.class);
        // }

        if (preview == null) {
            handleNoPreview(res, xpath, null);
            return null;
        }

        List<Blob> previewBlobs;
        try {
            if (xpath.equals("default")) {
                previewBlobs = preview.getFilePreviewBlobs(blobPostProcessing);
            } else {
                previewBlobs = preview.getFilePreviewBlobs(xpath,
                        blobPostProcessing);
            }
        } catch (PreviewException e) {
            previewInProcessing.remove(targetDocument.getId());
            handleNoPreview(res, xpath, e);
            return null;
        }

        if (previewBlobs == null || previewBlobs.size() == 0) {
            handleNoPreview(res, xpath, null);
            return null;
        }
        return previewBlobs;
    }

    protected void handleNoPreview(Response res, String xpath, Exception e) {

        // Generate token
        String token = generateToken();

        // Load no-templates template
        File home = Environment.getDefault().getHome();
        FileBlob noPreviewTemplate = new FileBlob(new File(home + "/nuxeo.war/templates/no-preview.html.ftl"));

        Map<String, Object> params = new HashMap<>();
        for (Map.Entry<String, Object> prop : DocumentModelUtils.getProperties(targetDocument).entrySet()) {
            String key = prop.getKey();
            Object value = prop.getValue();
            params.put(key.replace(":", "_"), value);
        }
        params.put("doc", targetDocument);
        for (Map.Entry<Object, Object> prop : Framework.getProperties().entrySet()) {
            String key = (String) prop.getKey();
            Object value = prop.getValue();
            params.put(key.replace(".", "_"), value);
        }
        params.put("token", token);

        try {
            String templateContent = noPreviewTemplate.getString();
            String rendered = RenderingService.getInstance().getRenderer("ftl").render(templateContent, params);
            res.setEntity(rendered, MediaType.TEXT_HTML);
            HttpServletResponse response = getHttpResponse(res);
            response.setHeader("Content-Disposition", "inline");
        } catch (Exception e1) {
            LOG.error("Unable to make ftl rendering for no-templates document", e1);
            handleNoPreviewDefault(res, xpath, e);
        }
    }

    /**
     * Generate a token.
     *
     * @return token
     */
    private String generateToken() {
        TokenAuthenticationService tokenAuthService = Framework.getService(TokenAuthenticationService.class);
        return tokenAuthService.acquireToken("Administrator", "preview", "default", "default", "r");
    }

    protected void handleNoPreviewDefault(Response res, String xpath, Exception e) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body><center><h1>");
        if (e == null) {
            sb.append(resourcesAccessor.getMessages().get(
                    "label.not.available.templates")
                    + "</h1>");
        } else {
            sb.append(resourcesAccessor.getMessages().get(
                    "label.cannot.generated.templates")
                    + "</h1>");
            sb.append("<pre>Technical issue:</pre>");
            sb.append("<pre>Blob path: ");
            sb.append(xpath);
            sb.append("</pre>");
            sb.append("<pre>");
            sb.append(e.toString());
            sb.append("</pre>");
        }

        sb.append("</center></body></html>");
        if (e instanceof NothingToPreviewException) {
            // Not an error, don't log
        } else {
            LOG.error("Could not build templates for missing blob at " + xpath, e);
        }

        res.setEntity(sb.toString(), MediaType.TEXT_HTML);
        HttpServletResponse response = getHttpResponse(res);

        response.setHeader("Content-Disposition", "inline");
    }

    protected void handlePreview(Response res, Blob previewBlob, String mimeType)
            throws IOException {
        final File tempfile = File.createTempFile("nuxeo-previewrestlet-tmp",
                "");
        Framework.trackFile(tempfile, res);
        previewBlob.transferTo(tempfile);
        res.setEntity(new OutputRepresentation(null) {
            @Override
            public void write(OutputStream outputStream) throws IOException {
                FileUtils.copyFile(tempfile, outputStream);
                tempfile.delete();
            }
        });
        HttpServletResponse response = getHttpResponse(res);

        response.setHeader("Content-Disposition", "inline");
        response.setContentType(mimeType);
    }

}
