package org.athento.nuxeo.wf.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.wf.api.RoutingConstants;
import org.athento.nuxeo.wf.utils.SchemaUtils;
import org.athento.nuxeo.wf.utils.WorkflowUtils;
import org.nuxeo.ecm.automation.core.operations.notification.MailTemplateHelper;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Hook for task assigned listener.
 */
public class TaskAssignedListener implements EventListener {

    private static final Log LOG = LogFactory
            .getLog(TaskAssignedListener.class);

    /**
     * Nuxeo URL.
     */
    private static final String NUXEO_URL = "nuxeo.url";

    /**
     * Handle notification.
     *
     * @param event
     * @throws Exception
     */
    @Override
    public void handleEvent(Event event) throws ClientException {
        if (event != null) {
            Map<String, Serializable> properties = event.getContext().getProperties();
            // Add host property
            properties.put("host", Framework.getProperty(NUXEO_URL));
            if (event.getContext() instanceof DocumentEventContext) {
                CoreSession session = event.getContext().getCoreSession();
                // Add task property from event document
                DocumentModel document = ((DocumentEventContext) event.getContext()).getSourceDocument();
                if (document != null) {
                    WorkflowUtils.initBindings(properties, event.getContext().getCoreSession(), document);
                    String taskId = WorkflowUtils.getTaskIdFromDocument(event.getContext());
                    String nodeId = WorkflowUtils.getNodeIdFromDocument(event.getContext());
                    if (taskId != null) {
                        Task task = WorkflowUtils.getTaskFromDocument(event.getContext());
                        String ignoreAssignment = task.getVariable(RoutingConstants.TASK_IGNORE_ASSIGNMENT_NOTIFICATION);
                        if ("true".equals(ignoreAssignment)) {
                            return;
                        }
                        // Set ACLs to Task
                        WorkflowUtils.setACLToTask(session, task);
                        // Set task id
                        properties.put("taskId", taskId);
                        String doctype = document.getType();
                        if (doctype.equals("Invoice")) {
                            // Set node Id to avoid loading unknown properties in preTask
                            properties.put("nodeId", nodeId);
                            // TODO: Right now we put into properties valid and known metatadata, but this might be
                            // improved so all properties are loaded, and then the template dedices what to use
                            properties.put("docTotalAmount", document.getPropertyValue("S_FACTURA:totalAmount"));
                            properties.put("docSubject", document.getPropertyValue("S_FACTURA:subject"));
                            if (!nodeId.equals("preTask")) {
                                // In preTask, relation to the project is still not known so these next 
                                // properties can not be loaded
                                properties.put("docProjectid", document.getPropertyValue("projectFile:projectid"));
                                DocumentModel project = session.getDocument(new IdRef((String) document.getPropertyValue("projectFile:projectDocid")));
                                properties.put("projectDocid", project.getId());
                                properties.put("projectBudget", project.getPropertyValue("invoicing:budget"));
                                properties.put("projectRemainingBudget", project.getPropertyValue("invoicing:remainingBudget"));
                            }
                        }
                        if (doctype.equals("ProjectFile")) {
                            // Set node Id to avoid loading unknown properties in preTask
                            properties.put("nodeId", nodeId);
                            // TODO: Right now we put into properties valid and known metatadata, but this might be
                            // improved so all properties are loaded, and then the template dedices what to use

                            properties.put("docDocid", document.getId());

                            // Invoicing values

                            properties.put("docBalance", document.getPropertyValue("invoicing:balance"));
                            properties.put("docBonusProvider", document.getPropertyValue("invoicing:bonusProvider"));
                            properties.put("docBudget", document.getPropertyValue("invoicing:budget"));
                            properties.put("docBudgetBonus", document.getPropertyValue("invoicing:budgetBonus"));
                            properties.put("docImputation", document.getPropertyValue("invoicing:imputation"));
                            properties.put("docInvoiceComments", document.getPropertyValue("invoicing:invoiceComments"));
                            properties.put("docOperativeBudget", document.getPropertyValue("invoicing:operativeBudget"));
                            properties.put("docRegion", document.getPropertyValue("invoicing:region"));
                            properties.put("docSociety", document.getPropertyValue("invoicing:society"));

                            // Marketing values

                            properties.put("docCampaignid", document.getPropertyValue("marketing:campaignid"));
                            properties.put("docCampaignDescription", document.getPropertyValue("marketing:campaignDescription"));
                            properties.put("docCampaignName", document.getPropertyValue("marketing:campaignName"));
                            properties.put("docMaterial", document.getPropertyValue("marketing:material"));
                            properties.put("docQuantity", document.getPropertyValue("marketing:quantity"));

                            // ProjectFile values

                            properties.put("docActivity", document.getPropertyValue("projectFile:activity"));
                            properties.put("docCategory", document.getPropertyValue("projectFile:category"));
                            properties.put("docEndPlannedDate", document.getPropertyValue("projectFile:endPlannedDate"));
                            properties.put("docInitialid", document.getPropertyValue("projectFile:initialid"));
                            properties.put("docProjectid", document.getPropertyValue("projectFile:projectid"));
                            properties.put("docProjectName", document.getPropertyValue("projectFile:projectName"));
                            properties.put("docProviders", document.getPropertyValue("projectFile:providers"));
                            properties.put("docSolicitantid", document.getPropertyValue("projectFile:solicitantid"));
                            properties.put("docStartPlannedDate", document.getPropertyValue("projectFile:startPlannedDate"));
                            properties.put("docSummary", document.getPropertyValue("projectFile:summary"));

                        }
                        // Check document content
                        if (WorkflowUtils.hasContent(document)) {
                            // Set preview url
                            properties.put("previewUrl", "/restAPI/athpreview/default/" + document.getId()
                                    + "/file:content/?token=" + WorkflowUtils.generatePreviewToken(document));
                        }
                        // Override url with workflow tab
                        properties.put("docUrl",
                                MailTemplateHelper.getDocumentUrl(document, "view_documents") + "?tabIds=:TAB_ROUTE_WORKFLOW");
                        // Set back to
                        properties.put("backUrl", "/");
                        // Add access token
                        String actors[] = WorkflowUtils.getTaskPrincipals(event.getContext());
                        HashMap<String, String> tokens = WorkflowUtils.generateAccessTokensAuth(actors);
                        if (!tokens.isEmpty()) {
                            // FIXME: Now, it is only for one user by group
                            properties.put("token",
                                    tokens.values().iterator().next());
                        }
                        properties.put("tokens", tokens);
                        if (task != null) {
                            // Check task step
                            DocumentModel taskNode = WorkflowUtils.getDocumentNodeFromTask(session, task);
                            if (taskNode != null) {
                                DocumentModel taskRoute = WorkflowUtils.getTaskRouteDocumentModel(task, session);
                                addNextTaskVariables(session, document, task, taskNode, taskRoute);
                            }
                        }
                        // Notifications
                        manageNotifications(session, event, taskId, properties, document);
                    }
                }
            }
        }
    }

    /**
     * Add next task variables.
     *
     * @param document
     * @param taskNodeDocumentModel
     */
    private void addNextTaskVariables(CoreSession session, DocumentModel document, Task task, DocumentModel taskNodeDocumentModel, DocumentModel routeDocumentModel) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Copying metadata from " + document.getRef() + " to taskNode " + taskNodeDocumentModel.getName() + ", " + taskNodeDocumentModel.getId());
        }
        // Manage metadata assignments
        if (LOG.isInfoEnabled()) {
            LOG.info("Task node doc to check metadataAssignments " + taskNodeDocumentModel.getId() + ", " + taskNodeDocumentModel.getName());
        }
        manageMetadataAssignments(session, task, taskNodeDocumentModel);
        // Propagate metadata from document to Task node document.
        List<String> docSchemas = SchemaUtils.getValidPropagatedSchemas(document.getSchemas());
        // Copy metadatas
        Map<String, Serializable> copiedMetadatas = WorkflowUtils.copyMetadatas(document, taskNodeDocumentModel, docSchemas.toArray(new String[0]));
        String nodeId = (String) taskNodeDocumentModel.getPropertyValue("rnode:nodeId");
        try {
            String metadataAssignment = (String) taskNodeDocumentModel.getPropertyValue("var-" + nodeId + ":metadataAssignment");
            if (metadataAssignment != null && !metadataAssignment.isEmpty()) {
                // Current metadata assignment
                copiedMetadatas.put("metadataAssignment", "#" + metadataAssignment);
            }
        } catch (PropertyNotFoundException e) {
            // DO NOTHING
        }
        // Get graph node for task and set variables
        GraphNode taskNode = taskNodeDocumentModel.getAdapter(GraphNode.class);
        taskNode.setVariables(copiedMetadatas);
        // Add variables to Route facet-var
        GraphRoute route = routeDocumentModel.getAdapter(GraphRoute.class);
        route.setVariables(copiedMetadatas);
        try {
            // Add order and priority
            Long metadataOrder = (Long) taskNodeDocumentModel.getPropertyValue("var-" + nodeId + ":order");
            if (metadataOrder != null) {
                task.getDocument().setPropertyValue("athtask:order", metadataOrder);
            }
            Long metadataPriority = (Long) taskNodeDocumentModel.getPropertyValue("var-" + nodeId + ":priority");
            if (metadataPriority != null) {
                task.getDocument().setPropertyValue("athtask:priority", metadataPriority);
            }
        } catch (PropertyNotFoundException e) {
            // DO NOTHING
        }
        // save task document
        session.saveDocument(task.getDocument());
    }

    /**
     * Manage metadata assignments on task assignation.
     *
     * @param session
     * @param task
     * @param taskNodeDoc
     */
    private void manageMetadataAssignments(CoreSession session, Task task, DocumentModel taskNodeDoc) {
        try {
            Map<String, Serializable> taskInfo = WorkflowUtils.getTaskInfo(session, task, true);
            String nodeId = (String) taskNodeDoc.getPropertyValue("rnode:nodeId");
            String metadataAssignment = (String) taskInfo.get("metadataAssignment");
            if (LOG.isInfoEnabled()) {
                LOG.info("Metadata assignment to execute " + metadataAssignment);
            }
            if (metadataAssignment != null && metadataAssignment.startsWith("#")) {
                DocumentModel targetDoc = session.getDocument(new IdRef(task.getTargetDocumentsIds().get(0)));
                String[] assignments = metadataAssignment.replace("#", "").split(",");
                for (String assignment : assignments) {
                    String[] assignmentInfo = assignment.split("=");
                    if (assignmentInfo.length == 2) {
                        try {
                            String propertyDoc = assignmentInfo[0];
                            String propertyTask = assignmentInfo[1];
                            LOG.info("Property " + propertyTask + " to " + propertyDoc);
                            String sourceValue = (String) taskInfo.get(propertyTask);
                            targetDoc.setPropertyValue(propertyDoc, sourceValue);
                            session.saveDocument(targetDoc);
                            LOG.info("Save assignment success");
                        } catch (PropertyException e) {
                            LOG.warn("Property is not found in the assignment", e);
                        }
                    }
                }
            }
        } catch (PropertyException e) {
            LOG.warn("Property metadataAssignment is not found for taskNode " + taskNodeDoc.getId(), e);
        }
    }

    /**
     * Get schemas of node.
     *
     * @param taskDocumentModel
     * @return
     */
    private String[] getNodeSchema(DocumentModel taskDocumentModel) {
        List<String> schemas = new ArrayList<String>();
        for (String schema : taskDocumentModel.getSchemas()) {
            if (schema.startsWith("var_")) {
                schemas.add(schema);
            }
        }
        return schemas.toArray(new String[0]);
    }

    /**
     * Manage notifications after assignation.
     *
     * @param session
     * @param event
     * @param taskId
     * @param properties
     * @param document
     */
    private void manageNotifications(CoreSession session, Event event, String taskId, Map<String, Serializable> properties, DocumentModel document) {
        // Notify the task assignation
        DocumentModel taskDoc = session.getDocument(new IdRef(taskId));
        if (taskDoc != null) {
            Task task = taskDoc.getAdapter(Task.class);
            List<String> actors = task.getActors();
            LOG.info("Notifying task assignment for " + task.getId() + " to " + actors);
            for (String actor : actors) {
                WorkflowUtils.sendEmailNotification(session, task, document, actor, properties);
            }
        }
        // Notify subscribers
        boolean onlyNotifyOnChanges = WorkflowUtils.readConfigValue(event.getContext().getCoreSession(),
                "extendedWF:onlyNotifyOnChanges", Boolean.class);
        if (!onlyNotifyOnChanges) {
            // Send email to others
            boolean autoSubscribe = WorkflowUtils.readConfigValue(event.getContext().getCoreSession(),
                    "extendedWF:autoSubscribeCreator", Boolean.class);
            if (autoSubscribe) {
                if (taskDoc != null) {
                    try {
                        String processId = (String) taskDoc.getPropertyValue("nt:processId");
                        DocumentModel processInstance = session.getDocument(new IdRef(processId));
                        String initiator = (String) processInstance.getPropertyValue("docri:initiator");
                        Map<String, Object> params = new HashMap<>();
                        params.putAll(properties);
                        params.put("taskId", taskId);
                        params.put("toUser", initiator);
                        params.put("template", "template:workflowTaskAssignedGeneric");
                        params.put("subject", "[Nuxeo]Task assigned in " + document.getName());
                        params.put("html", true);
                        WorkflowUtils.runOperation("Athento.SendNotificationTaskAssigned", document, params, session);
                    } catch (Exception e) {
                        LOG.error("Error sending notification to initiator", e);
                    }
                }
            }
            String autoSubscribeUsers = WorkflowUtils.readConfigValue(event.getContext().getCoreSession(),
                    "extendedWF:autoSubscribeUsers", String.class);
            if (autoSubscribeUsers != null) {
                if (taskDoc != null) {
                    String[] users = autoSubscribeUsers.split(",");
                    for (String user : users) {
                        try {
                            Map<String, Object> params = new HashMap<>();
                            params.putAll(properties);
                            params.put("taskId", taskId);
                            params.put("toUser", user.trim());
                            params.put("template", "template:workflowTaskAssignedGeneric");
                            params.put("subject", "[Nuxeo]Task assigned in " + document.getName());
                            params.put("html", true);
                            WorkflowUtils.runOperation("Athento.SendNotificationTaskAssigned", document, params, session);
                        } catch (Exception e) {
                            LOG.error("Error sending notification to user " + user, e);
                        }
                    }
                }
            }
        }
    }


}
