package org.athento.nuxeo.security.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.login.BaseLoginModule;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Delegated Login Plugin.
 */
public class DelegatedLoginPlugin extends BaseLoginModule {

    private static final Log LOG = LogFactory.getLog(DelegatedLoginPlugin.class);

    public static final String NAME = "Delegated_LM";

    public Boolean initLoginModule() {
        return Boolean.TRUE;
    }

    public String validatedUserIdentity(UserIdentificationInfo userIdent) {
        UserManager userManager = Framework.getService(UserManager.class);
        if (userIdent.getLoginParameters().get("delegated") != null
                && !userIdent.getLoginParameters().get("delegated").isEmpty()) {
            if (userManager.checkUsernamePassword(userIdent.getUserName(), userIdent.getPassword())) {
                return userIdent.getLoginParameters().get("delegated");
            }
        } else {
            if (userManager.checkUsernamePassword(userIdent.getUserName(), userIdent.getPassword())) {
                return userIdent.getUserName();
            }
        }
        return null;
    }

}
