package org.athento.nuxeo.security.authenticator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.security.api.SecurityConstants;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

/**
 * Basic authenticator based on Nuxeo but with securization.
 */
public class BasicAuthenticator implements NuxeoAuthenticationPlugin {

    protected static final String REALM_NAME_KEY = "RealmName";

    protected static final String FORCE_PROMPT_KEY = "ForcePromptURL";

    protected static final String AUTO_PROMPT_KEY = "AutoPrompt";

    protected static final String PROMPT_URL_KEY = "PromptUrl";

    protected static final String DEFAULT_REALMNAME = "Nuxeo 5";

    protected static final String BA_HEADER_NAME = "WWW-Authenticate";

    protected static final String EXCLUDE_URL_KEY = "ExcludeBAHeader";

    protected String realName;

    protected Boolean autoPrompt = false;

    protected List<String> forcePromptURLs;

    private List<String> excludedHeadersForBasicAuth;

    private Log LOG = LogFactory.getLog(BasicAuthenticator.class);

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        try {
            if (needToAddBAHeader(httpRequest)) {
                // forcing session invalidation
                HttpSession session = httpRequest.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                String baHeader = "Basic realm=\"" + realName + '\"';
                httpResponse.addHeader(BA_HEADER_NAME, baHeader);
            }
            int statusCode;
            Integer requestStatusCode = (Integer) httpRequest.getAttribute(NXAuthConstants.LOGIN_STATUS_CODE);
            if (requestStatusCode != null) {
                statusCode = requestStatusCode;
            } else {
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;
            }
            httpResponse.sendError(statusCode);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks if we need to include a basic auth header back to the client.
     *
     * @param httpRequest
     * @return true if we need to include the auth header
     * @since 5.9.2
     */
    private boolean needToAddBAHeader(HttpServletRequest httpRequest) {
        for (String header : excludedHeadersForBasicAuth) {
            if (StringUtils.isNotBlank(httpRequest.getHeader(header))) {
                return false;
            }
            if (httpRequest.getCookies() != null) {
                for (Cookie cookie : httpRequest.getCookies()) {
                    if (cookie.getName().equals(header)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
                                                         HttpServletResponse httpResponse) {

        String auth = httpRequest.getHeader(AUTHORIZATION);

        if (auth != null && auth.toLowerCase().startsWith("basic")) {
            int idx = auth.indexOf(' ');
            String b64userPassword = auth.substring(idx + 1);
            byte[] clearUp = Base64.decodeBase64(b64userPassword);
            String userCredentials = new String(clearUp);
            int idxOfColon = userCredentials.indexOf(':');
            if (idxOfColon > 0 && idxOfColon < userCredentials.length() - 1) {
                String username = userCredentials.substring(0, idxOfColon);
                String password = userCredentials.substring(idxOfColon + 1);
                // forcing session invalidation
                HttpSession session = httpRequest.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                return new UserIdentificationInfo(username, password);
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Check if user is systemic.

     * @param userName
     * @return
     */
    private boolean isSystemicUser(String userName) {
        UserManager um = Framework.getService(UserManager.class);
        List<String> users = um.getUsersInGroup(SecurityConstants.SYSTEMIC_GROUP);
        return users.contains(userName);
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        if (autoPrompt) {
            return true;
        } else {
            String requestedURI = httpRequest.getRequestURI();
            String context = httpRequest.getContextPath() + '/';
            requestedURI = requestedURI.substring(context.length());
            for (String prefixURL : forcePromptURLs) {
                if (requestedURI.startsWith(prefixURL)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void initPlugin(Map<String, String> parameters) {
        if (parameters.containsKey(REALM_NAME_KEY)) {
            realName = parameters.get(REALM_NAME_KEY);
        } else {
            realName = DEFAULT_REALMNAME;
        }

        if (parameters.containsKey(AUTO_PROMPT_KEY)) {
            autoPrompt = parameters.get(AUTO_PROMPT_KEY).equalsIgnoreCase("true");
        }

        forcePromptURLs = new ArrayList<String>();
        for (Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getKey().startsWith(FORCE_PROMPT_KEY)) {
                forcePromptURLs.add(entry.getValue());
            }
        }

        excludedHeadersForBasicAuth = new ArrayList<>();
        for (Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getKey().startsWith(EXCLUDE_URL_KEY)) {
                excludedHeadersForBasicAuth.add(entry.getValue());
            }
        }
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

}
