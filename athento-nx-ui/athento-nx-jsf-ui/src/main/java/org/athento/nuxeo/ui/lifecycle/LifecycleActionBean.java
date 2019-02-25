package org.athento.nuxeo.ui.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Lifecycle action.
 */
@Name("athLifecycleAction")
@Scope(ScopeType.CONVERSATION)
public class LifecycleActionBean implements LifecycleAction, Serializable {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(LifecycleActionBean.class);

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected CoreSession documentManager;

    private LifeCycleService lfService = null;

    /**
     * Selected transition.
     */
    private String selectedTransition;

    /**
     * Get available transitions for current document.
     *
     * @return
     */
    public Collection<String> getAvailableTransitions() {
        Collection<String> availableTransitions = new ArrayList<>();
        lfService = getLifecycleService();
        if (lfService == null) {
            LOG.error("Unable to get lifecycle service");
            return availableTransitions;
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            LifeCycle lifeCycle = lfService.getLifeCycleByName(currentDocument.getLifeCyclePolicy());
            availableTransitions.addAll(lifeCycle.getAllowedStateTransitionsFrom(currentDocument.getCurrentLifeCycleState()));
        }
        return availableTransitions;
    }

    /**
     * Get lifecycle service.
     *
     * @return
     */
    private LifeCycleService getLifecycleService() {
        if (lfService == null) {
            lfService = NXCore.getLifeCycleService();
        }
        return lfService;
    }

    /**
     * Execute the current transition.
     */
    @Override
    public void followTransition() {
        if (selectedTransition != null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Follow transition " + this.selectedTransition);
                }
                currentDocument.followTransition(this.selectedTransition);
            }
        }
    }

    public String getSelectedTransition() {
        return selectedTransition;
    }

    public void setSelectedTransition(String selectedTransition) {
        this.selectedTransition = selectedTransition;
    }

    /**
     * Check user is in group.
     *
     * @param group
     * @return
     */
    public boolean userInGroup(String group) {
        UserManager userManager = Framework.getService(UserManager.class);
        NuxeoGroup nxGroup = userManager.getGroup(group);
        if (nxGroup == null) {
            return false;
        }
        List<String> users = userManager.getUsersInGroupAndSubGroups(group);
        String username = documentManager.getPrincipal().getName();
        return users.contains(username);
    }
}
