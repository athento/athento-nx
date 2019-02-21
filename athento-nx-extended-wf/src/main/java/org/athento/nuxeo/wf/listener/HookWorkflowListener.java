package org.athento.nuxeo.wf.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.wf.utils.WorkflowUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerHook;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hook for any workflow event.
 */
public class HookWorkflowListener implements NotificationListenerHook {

    private static final Log LOG = LogFactory
            .getLog(HookWorkflowListener.class);

    /** Notification service. */
    private NotificationService notificationService = NotificationServiceHelper.getNotificationService();

    private UserManager userManager = null;

    /**
     * Handle notification.
     *
     * @param event
     * @throws Exception
     */
    @Override
    public void handleNotifications(Event event) throws ClientException {
        if (event != null && event.getContext() instanceof DocumentEventContext) {
            LOG.info("Notification Hook executing...");
            DocumentModel doc = ((DocumentEventContext) event.getContext()).getSourceDocument();
            Map<String, Serializable> properties = event.getContext().getProperties();
            Object recipientProperty = properties.get(NotificationConstants.RECIPIENTS_KEY);
            String[] recipients = null;
            if (recipientProperty != null) {
                if (recipientProperty instanceof String[]) {
                    recipients = (String[]) properties.get(NotificationConstants.RECIPIENTS_KEY);
                } else if (recipientProperty instanceof String ) {
                    recipients = new String[1];
                    recipients[0] = (String) recipientProperty;
                }
            }
            if (recipients == null) {
                return;
            }
            Set<String> users = new HashSet<String>();
            for (String recipient : recipients) {
                if (recipient == null) {
                    continue;
                }
                if (recipient.contains(NuxeoPrincipal.PREFIX)) {
                    users.add(recipient.replace(NuxeoPrincipal.PREFIX, ""));
                } else if (recipient.contains(NuxeoGroup.PREFIX)) {
                    List<String> groupMembers = getGroupMembers(recipient.replace(
                            NuxeoGroup.PREFIX, ""));
                    for (String member : groupMembers) {
                        users.add(member);
                    }
                } else {
                    if (NotificationServiceHelper.getUsersService().getGroup(
                            recipient) != null) {
                        users.addAll(getGroupMembers(recipient));
                    } else {
                        users.add(recipient);
                    }
                }

            }
            if (!users.isEmpty()) {
                // Add email to audit
                String comment = "Email sent to " + WorkflowUtils.getStringFromSet(users);
                WorkflowUtils.newEntry(doc,
                        event.getContext().getPrincipal().getName(), "emailSent", DocumentEventCategories.EVENT_DOCUMENT_CATEGORY, comment, null, null);
            }
        }
    }

    /**
     * Get group members.
     *
     * @param groupId
     * @return
     * @throws ClientException
     */
    protected List<String> getGroupMembers(String groupId)
            throws ClientException {
        return getUserManager().getUsersInGroupAndSubGroups(groupId);
    }

    /**
     * Get user manager.
     *
     * @return
     */
    protected UserManager getUserManager() {
        if (userManager == null) {
            try {
                userManager = Framework.getService(UserManager.class);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "UserManager service not deployed.", e);
            }
        }
        return userManager;
    }
}
