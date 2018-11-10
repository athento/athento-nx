package org.athento.nuxeo.security.authenticator;

import com.auth0.jwt.interfaces.Claim;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.security.api.SecurityConstants;
import org.athento.nuxeo.security.util.JWTHelper;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

/**
 * Systemic delegated authenticator. It is based on {@link com.auth0.jwt.JWT}.
 * It is used to authenticate with an systemic user existing in "systemic" group
 * and login with a delegated user.
 * To manage access, this authenticator use a Bearer token near the username
 * in Authenticator header.
 */
public class SystemicDelegatedAuthenticator implements NuxeoAuthenticationPlugin {

    protected static final String BEARER_SP = "bearer";

    private Log LOG = LogFactory.getLog(SystemicDelegatedAuthenticator.class);

    @Override
    public void initPlugin(Map<String, String> parameters) {
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        return false;
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return false;
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
                                                         HttpServletResponse httpResponse) {

        String auth = httpRequest.getHeader(AUTHORIZATION);

        if (auth != null && auth.toLowerCase().startsWith(BEARER_SP)) {
            // Get username and token
            String [] authValues = auth.split(" ");
            if (authValues.length != 2) {
                LOG.warn("Systemic login is trying but header is not well formatted.");
                return null;
            }
            String token = authValues[1];
            Map<String, Claim> claims = JWTHelper.verifyToken(token, null);
            if (claims == null) {
                LOG.warn("Bearer token was invalid");
                return null;
            }
            Claim userClaim = claims.get(SecurityConstants.CLAIM_SUBJECT);
            String username = userClaim.asString();
            if (!(username instanceof String)) {
                LOG.warn("Username must be an string in Systemic auth");
                return null;
            }
            // Check systemic user
            if (isSystemicUser(username)) {
                // Get delegated
                Claim delegatedUserClaim = claims.get(SecurityConstants.CLAIM_DELEGATED_USER);
                String delegatedUser = delegatedUserClaim.asString();
                if (delegatedUser != null && existsUser(delegatedUser)) {
                    username = delegatedUser;
                }
            } else {
                LOG.warn("Try login with username " + username + " and it is not a systemic user");
                return null;
            }
            return new UserIdentificationInfo(username, username);
        }
        return null;
    }

    /**
     * Check if username exists.
     *
     * @param username
     * @return
     */
    private boolean existsUser(String username) {
        UserManager um = Framework.getService(UserManager.class);
        if (um == null) {
            return true;
        }
        return um.getUserModel(username) != null;
    }

    /**
     * Check if user is systemic.

     * @param userName
     * @return
     */
    private boolean isSystemicUser(String userName) {
        UserManager um = Framework.getService(UserManager.class);
        if (um == null) {
            return true;
        }
        List<String> users = um.getUsersInGroup(SecurityConstants.SYSTEMIC_GROUP);
        return users.contains(userName);
    }



}
