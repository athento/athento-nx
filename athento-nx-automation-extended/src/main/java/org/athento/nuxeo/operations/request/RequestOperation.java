package org.athento.nuxeo.operations.request;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.nuxeo.ecm.automation.core.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Abstract Request Operation class.
 */
public abstract class RequestOperation {

    /**
     * Make Get call using URL and properties parameters.
     * Replace attributes and add the request parameters in the URL.
     *
     * @param url
     * @param attributes
     * @param parameters
     * @return
     * @throws UnsupportedEncodingException
     */
    protected String makeGetCall(String url, Properties attributes, Properties parameters) throws UnsupportedEncodingException {
        String tmpUrl = url;
        if (attributes != null) {
            // Replace attributes
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                String attributeKey = attribute.getKey();
                String attributeValue = attribute.getValue();
                if (attributeKey != null) {
                    attributeKey = URLEncoder.encode(attributeKey, "UTF-8");
                    tmpUrl = tmpUrl.replaceAll("\\{" + attributeKey + "\\}", attributeValue);
                }
            }
        }
        if (parameters != null) {
            if (!parameters.isEmpty()) {
                tmpUrl = tmpUrl + "?";
            }
            // Add request params
            int paramPos = 0;
            for (Map.Entry<String, String> property : parameters.entrySet()) {
                String propertyKey = property.getKey();
                String propertyValue = property.getValue();
                if (propertyKey != null) {
                    propertyKey = URLEncoder.encode(propertyKey, "UTF-8");
                    propertyValue = URLEncoder.encode(propertyValue, "UTF-8");
                    tmpUrl += propertyKey + "=" + propertyValue;
                    if (paramPos++ < parameters.size() - 1) {
                        tmpUrl += "&";
                    }
                }
            }
        }
        return tmpUrl;
    }

    /**
     * Create accept self signed certificate client.
     * @return
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    protected CloseableHttpClient createAcceptSelfSignedCertificateClient()
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        SSLContext sslContext = SSLContextBuilder
                .create()
                .loadTrustMaterial(new TrustSelfSignedStrategy())
                .build();
        HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
        SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);
        return HttpClients
                .custom()
                .setSSLSocketFactory(connectionFactory)
                .build();
    }

    /**
     * Check valid URL.
     *
     * @param callUrl
     * @return
     */
    protected boolean isValidUrl(String callUrl) {
        try {
            new URL(callUrl);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
