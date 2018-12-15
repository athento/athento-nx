package org.athento.nuxeo.operations.request;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;

import java.io.IOException;

/**
 * Make a GET request.
 *
 * @author victorsanchez
 */
@Operation(id = GetRequestOperation.ID, category = "Athento", label = "Execute a GET request", description = "Execute a GET request",
        since = "7.10", addToStudio = false)
public class GetRequestOperation extends RequestOperation {

    /**  Log. */
    private static final Log LOG = LogFactory.getLog(GetRequestOperation.class);

    /**
     * Operation ID.
     */
    public static final String ID = "Athento.GetRequest";

    /**
     * SE URL pattern.
     *
     * Use {param} format to define properties to parse into URL.
     * Example: http
     */
    @Param(name = "url")
    protected String url;

    /**
     * Attributes to parse.
     *
     * It will be parsed into URL parameter.
     */
    @Param(name = "attributes", required = false)
    protected Properties attributes;

    /**
     * Parameters are the request params.
     *
     * It will be added as request parameters.
     */
    @Param(name = "parameters", required = false)
    protected Properties parameters;

    /**
     * Xpath to save the GET result into input document.
     */
    @Param(name = "xpath", required = false)
    protected String xpath;

    /**
     * Save param indicates result will be saved into xpath param into input document.
     */
    @Param(name = "save", required = false)
    protected boolean save = false;

    /**
     * Session.
     */
    @Context
    protected CoreSession session;

    /**
     * Run, get request call.
     *
     * @param doc is the source document
     * @return get the request response
     * @throws RequestException on error
     */
    @OperationMethod
    public String run(DocumentModel doc) throws RequestException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Running Get operation...");
        }
        try {
            // Make the call
            String callUrl = makeGetCall(url, attributes, parameters);
            if (!isValidUrl(callUrl)) {
                throw new RequestException("Please check your URL: " + callUrl);
            }
            // Execute request
            String response = executeRequest(callUrl);
            // Check to save
            if (save) {
                if (xpath == null) {
                    LOG.warn("xpath value is mandatory to save request result into the document.");
                } else {
                    try {
                        doc.setPropertyValue(xpath, response);
                        session.saveDocument(doc);
                    } catch (PropertyException e) {
                        LOG.warn("Unable to save response into xpath", e);
                    }
                }
            }
            return response;
        } catch (Exception e) {
            LOG.error("Unable to execute GET request.", e);
            throw new RequestException("Unable to execute GET request : " + e.getMessage(), e);
        }
    }

    /**
     * Get documents requested form.
     *
     * @param targetUrl
     * @return
     */
    public final String executeRequest(String targetUrl) {
        if (targetUrl == null) {
            return null;
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Execute GET call to: " + targetUrl + "...");
        }
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = null;
        try {
            if (targetUrl.startsWith("https")) {
                LOG.info("Https...");
                httpClient = createAcceptSelfSignedCertificateClient();

            } else {
                httpClient = HttpClients.custom()
                        .disableContentCompression()
                        .useSystemProperties().build();
            }
            HttpGet httpGet = new HttpGet(targetUrl);
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                LOG.error("Unable to get response from server");
                return "{\"status\": " + response.getStatusLine().getStatusCode() +
                        ", \"message\": \"" + response.getStatusLine().getReasonPhrase() + "\"}";
            } else {
                String result = IOUtils.toString(response.getEntity().getContent(), "utf-8");
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Server response was: " + result);
                }
                return result;
            }
        } catch (Exception e) {
            LOG.error("Unable to execute GET request", e);
            return "{\"status\":  500, \"message\": \"" + e.getMessage() + "\"}";
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



}
