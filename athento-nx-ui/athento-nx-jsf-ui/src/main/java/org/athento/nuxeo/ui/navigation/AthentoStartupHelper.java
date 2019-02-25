
package org.athento.nuxeo.ui.navigation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.*;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.dashboard.DashboardNavigationHelper;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.StartupHelper;

import javax.faces.context.FacesContext;
import java.security.Principal;

import static org.jboss.seam.ScopeType.SESSION;

/**
 * Athento startup helper based on {@link StartupHelper} of Nuxeo DM.
 */
@Name("athentoStartupHelper")
@Scope(SESSION)
@Install(precedence = Install.FRAMEWORK)
public class AthentoStartupHelper extends StartupHelper {

    private static final Log LOG = LogFactory.getLog(AthentoStartupHelper.class);

    @In(create = true)
    protected transient RepositoryManager repositoryManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In
    protected transient Context sessionContext;

    @In(create = true)
    protected DashboardNavigationHelper dashboardNavigationHelper;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    /**
     * Redirect to dashboard.
     *
     * @return
     */
    @Begin(id = "#{conversationIdGenerator.nextMainConversationId}", join = true)
    public String gotoDashboard() {
        try {
            setupCurrentUser();
            // we try to select the server to go to the next screen
            if (navigationContext.getCurrentServerLocation() == null) {
                // update location
                RepositoryLocation repLoc = new RepositoryLocation(repositoryManager.getDefaultRepositoryName());
                navigationContext.setCurrentServerLocation(repLoc);
            }

            if (documentManager == null) {
                documentManager = navigationContext.getOrCreateDocumentManager();
            }

            if (Events.exists()) {
                Events.instance().raiseEvent(EventNames.USER_SESSION_STARTED, documentManager);
            }
            return dashboardNavigationHelper.navigateToDashboard();

        } catch (NuxeoException e) {
            LOG.error("error while initializing the Seam context with a CoreSession instance: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Set the current user from Seam context.
     */
    public void setupCurrentUser() {
        Principal currentUser = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        sessionContext.set("currentUser", currentUser);
    }

}
