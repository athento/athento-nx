package org.athento.nuxeo.security.core;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.security.api.SecurityConstants;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;



/**
 * Security service.
 */
public class SecurityServiceImpl extends DefaultComponent {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(SecurityServiceImpl.class);

    /**
     * Manage application started.
     *
     * @param context
     */
    @Override
    public void applicationStarted(ComponentContext context) {

        // Create systemic group (if it does not exist)
        createSystemicGroup();

    }

    /**
     * Create systemic group.
     */
    private void createSystemicGroup() {
        UserManager um = Framework.getService(UserManager.class);
        if (um == null) {
            LOG.trace("User manager is not initialized");
            return;
        }
        try {
            DocumentModel groupModel = um.getBareGroupModel();
            String schemaName = um.getGroupSchemaName();
            groupModel.setProperty(schemaName, "groupname", SecurityConstants.SYSTEMIC_GROUP);
            groupModel.setProperty(schemaName, "grouplabel", SecurityConstants.SYSTEMIC_GROUP);
            groupModel.setProperty(schemaName, "description", "Systemic group of users");
            um.createGroup(groupModel);
        } catch (GroupAlreadyExistsException e) {
            // DO NOTHING
        }
    }
}
