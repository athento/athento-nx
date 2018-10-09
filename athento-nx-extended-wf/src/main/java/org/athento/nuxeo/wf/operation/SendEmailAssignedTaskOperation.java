package org.athento.nuxeo.wf.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.features.PlatformFunctions;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Operation to send an email to task assigned user or group.
 */
@Operation(id = SendEmailAssignedTaskOperation.ID, category = "Athento", label = "Send notification email to assigned task", description = "Send notification to remember an assigned task.")
public class SendEmailAssignedTaskOperation {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(SendEmailAssignedTaskOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.SendNotificationTaskAssigned";

    /** Operation context. */
    @Context
    protected OperationContext ctx;

    /** Core session. */
    @Context
    protected CoreSession session;

    @Param(name = "from", required = false)
    protected String from = "noreply@athento.com";

    @Param(name = "template", required = false, values = { "template:routingTaskAssigned", "template:workflowTaskAssignedProjectFile" })
    protected String template = "template:routingTaskAssigned";

    @Param(name = "subject", required = false)
    protected String subject;

    @Param(name = "html", required = false)
    protected boolean html = false;

    @Param(name = "toUser", required = false,
            description = "If this value is not empty, the task assigned notification will be send to the this username.")
    protected String toUser;

    @Param(name = "taskId", required = false)
    protected String taskId;

    @Param(name = "files", required = false)
    protected StringList files;

    /**
     * Run operation for a document.
     *
     * @param doc is the document
     * @return the document
     * @throws Exception on error
     */
    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {
        boolean ignoreNotifications = Boolean.valueOf(Framework.getProperty("athento.workflow.ignoreNotification", "false"));
        if (ignoreNotifications) {
            return doc;
        }
        TaskService taskService = Framework.getService(TaskService.class);
        if (taskId != null) {
            Task task = taskService.getTask(session, taskId);
            notifyTask(task, doc);
        } else {
            List<Task> documentTasks = taskService.getTaskInstances(doc, (NuxeoPrincipal) null, session);
            for (Task task : documentTasks) {
                notifyTask(task, doc);
            }
        }
        return doc;
    }

    /**
     * Notifiy task.
     *
     * @param task
     * @param doc
     * @throws Exception
     */
    private void notifyTask(Task task, DocumentModel doc) throws Exception {
        HashMap<String, Serializable> params = new HashMap<>();
        for (Map.Entry<String, Object> entry : ctx.entrySet()) {
            if (entry.getValue() instanceof Serializable) {
                params.put(entry.getKey(), (Serializable) entry.getValue());
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending notification for document:" + doc.getId() + " and task: " + task.getDocument().getId());
        }

        // Check subject
        if (subject == null) {
            subject = doc.getTitle();
        }

        List<String> actors = task.getActors();
        if (actors != null) {
            StringList usernameList = new StringList();
            StringList groupList = new StringList();
            for (String act : actors) {
                if (act.startsWith("user:")) {
                    usernameList.add(act.replace("user:", ""));
                } else if (act.startsWith("group:")) {
                    groupList.add(act.replace("group:", ""));
                } else {
                    usernameList.add(act);
                }
            }
            try {
                // Prepare notification
                Map<String, Object> mailParams = new HashMap<>();
                mailParams.put("from", from);
                mailParams.put("message", template);
                mailParams.put("subject", subject);
                mailParams.put("HTML", html);
                mailParams.put("files", files);
                if (toUser != null) {
                    if (toUser.startsWith("user:")) {
                        // Send to user
                        toUser = toUser.replace("user:", "");
                        mailParams.put("to", new PlatformFunctions().getEmail(toUser));
                    } else if (toUser.startsWith("group:")) {
                        toUser = toUser.replace("group:", "");
                        // Send to each user
                        mailParams.put("to", new PlatformFunctions().getEmailsFromGroup(toUser));
                    } else {
                        mailParams.put("to", new PlatformFunctions().getEmail(toUser));
                    }
                    if (mailParams.containsKey("to")) {
                        // Run send email operation to the param "to" email
                        runOperation("Notification.SendMail", doc, mailParams, session, ctx);
                    }
                } else {
                    try {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Sending with no user destination...");
                        }
                        if (!usernameList.isEmpty()) {
                            if (LOG.isInfoEnabled()) {
                                LOG.info("Sending task notification to users: " + usernameList);
                            }
                            mailParams.put("to", new PlatformFunctions().getEmails(usernameList));
                            if (LOG.isInfoEnabled()) {
                                LOG.info("Mail params=" + mailParams);
                            }
                            if (mailParams.containsKey("to")) {
                                // Run send email operation to username list
                                runOperation("Notification.SendMail", doc, mailParams, session, ctx);
                            }
                        }
                        if (!groupList.isEmpty()) {
                            for (String group : groupList) {
                                if (LOG.isInfoEnabled()) {
                                    LOG.info("Sending task notification to group: " + group);
                                }
                                mailParams.put("to", new PlatformFunctions().getEmailsFromGroup(group));
                                if (LOG.isInfoEnabled()) {
                                    LOG.info("Mail params=" + mailParams);
                                }
                                // Run send email operation to each group
                                runOperation("Notification.SendMail", doc, mailParams, session, ctx);
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("Error sending email notification", e);
                    }
                }
            } catch (Exception e) {
                LOG.error("Notification error", e);
            }
        }
    }

    /**
     * Run operation.
     *
     * @param operationId
     * @param input
     * @param params
     * @param session
     * @return
     * @throws Exception
     */
    private static Object runOperation(String operationId, Object input,
                                      Map<String, Object> params, CoreSession session, OperationContext context)
            throws Exception {
        AutomationService automationManager = Framework
                .getLocalService(AutomationService.class);
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(input);
        ctx.putAll(context);
        return automationManager.run(ctx, operationId, params);
    }
}
