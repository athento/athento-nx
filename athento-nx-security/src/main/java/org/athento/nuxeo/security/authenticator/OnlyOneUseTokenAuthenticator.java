package org.athento.nuxeo.security.authenticator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.web.common.CookieHelper;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Only one use token authenticator. Based on {@link org.nuxeo.ecm.platform.ui.web.auth.token.TokenAuthenticator}.
 */
public class OnlyOneUseTokenAuthenticator implements NuxeoAuthenticationPlugin {

    private static final Log LOG = LogFactory.getLog(OnlyOneUseTokenAuthenticator.class);

    private TokenAuthenticationService tokenAuthService;

    protected static final String TOKEN_FIELD = "token";
    protected static final String TOKEN_HEADER = "X-Authentication-Token";

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return false;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String token = getToken(httpRequest);

        if (token == null) {
            return null;
        }

        String userName = getUserByToken(token);
        if (userName == null) {
            LOG.warn(String.format(
                    "No user bound to token."));
            return null;
        }

        Cookie cookie = CookieHelper.createCookie(httpRequest, TOKEN_HEADER, token);
        httpResponse.addCookie(cookie);

        // Revoke token
        revokeToken(token);

        return new UserIdentificationInfo(userName, userName);
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return false;
    }

    @Override
    public void initPlugin(Map<String, String> parameters) {}

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    /**
     * Revoke token.
     *
     * @param token to revoke
     */
    protected void revokeToken(String token) {
        getTokenAuthService().revokeToken(token);
    }

    /**
     * Get token.
     *
     * @param httpRequest
     * @return
     */
    private String getToken(HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader(TOKEN_HEADER);

        if (token == null) {
            token = httpRequest.getParameter(TOKEN_FIELD);
        }

        if (token == null && httpRequest.getCookies() != null) {
            Cookie cookie = getTokenFromCookie(httpRequest);
            if (cookie != null) {
                return cookie.getValue();
            }
        }
        return token;
    }

    /**
     * Get token from cookie.
     *
     * @param httpRequest
     * @return
     */
    private Cookie getTokenFromCookie(HttpServletRequest httpRequest) {
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(TOKEN_HEADER)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     * Get user by token.
     *
     * @param token
     * @return
     */
    protected String getUserByToken(String token) {
        return getTokenAuthService().getUserName(token);
    }

    /**
     * Get token auth service.
     *
     * @return
     */
    private TokenAuthenticationService getTokenAuthService() {
        if (tokenAuthService == null) {
            tokenAuthService = Framework.getLocalService(TokenAuthenticationService.class);
        }
        return tokenAuthService;
    }
}
