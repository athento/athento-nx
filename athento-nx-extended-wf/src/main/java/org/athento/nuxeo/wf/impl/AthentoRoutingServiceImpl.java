package org.athento.nuxeo.wf.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.wf.api.AthentoRoutingService;
import org.athento.nuxeo.wf.api.RoutingConstants;
import org.athento.nuxeo.wf.api.UpgradeInfo;
import org.athento.nuxeo.wf.exception.UpgradeWorkflowException;
import org.athento.nuxeo.wf.utils.WorkflowExtConstants;
import org.athento.nuxeo.wf.utils.WorkflowUtils;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.core.audit.RoutingAuditHelper;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import java.io.Serializable;
import java.util.*;

/**
 * Athento Routing Service.
 */
public class AthentoRoutingServiceImpl extends DefaultComponent implements AthentoRoutingService {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(AthentoRoutingServiceImpl.class);

    /**
     * Upgrade route to the last deployed version.
     *
     * @param route
     * @param doc
     * @param session
     * @throws UpgradeWorkflowException
     */
    @Override
    public void upgradeRoute(DocumentRoute route, DocumentModel doc, CoreSession session) throws UpgradeWorkflowException {
        LOG.info("Upgrading route: " + route.getModelName() + ", " + route.getModelId());
        // Get last instance route for docRoute
        DocumentRoute lastVersionRoute = getLastVersionRoutedDeployed(route, session);
        if (lastVersionRoute.getDocument().getId().equals(route.getDocument().getId())) {
            throw new UpgradeWorkflowException("There is no version to upgrade for route " + route.getTitle());
        }
        String currentRouteVersion = WorkflowUtils.getRouteVersion(route);
        String workflowTitle = route.getDocument().getTitle();
        // Get last deployed workflow version
        String workflowVersion = WorkflowUtils.getRouteVersion(lastVersionRoute);
        LOG.info("Last route deployed for " + workflowTitle + " is " + lastVersionRoute.getDocument().getId()
                + " with version " + workflowVersion);
        // Create instance for last version route
        DocumentRoutingService documentRoutingService = getDocumentRoutingService();
        Map<String, Serializable> vars = new HashMap<String, Serializable>();
        vars.put(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON, Boolean.TRUE);
        List<String> ids = new ArrayList<>();
        ids.add(doc.getId());
        String newWorkflowId = documentRoutingService.createNewInstance(lastVersionRoute.getModelName(),
                ids, vars, session, Boolean.TRUE);
        // Get new route instance
        DocumentModel newRouteDoc = session.getDocument(new IdRef(newWorkflowId));
        DocumentRoute newRoute = newRouteDoc.getAdapter(DocumentRoute.class);
        LOG.info("New workflow instance created: " + newRoute.getDocument().getId());
        // Copy all workflowVars from lastRoute into currentRoute
        copyWorkflowVars(route, newRoute, workflowTitle);
        // Get open tasks for current route
        List<Task> openLastTasks = getOpenTask(route, doc, session);
        for (Task openTask : openLastTasks) {
            LOG.info("Task is open: "  + openTask.getName() + ", " + openTask.getId());
        }
        // Synchronize open task into current route
        synchronizeOpenTask(openLastTasks, newRoute, doc, session);
        // Close open task for current route
        closeOpenTask(route, doc, session);
        // Add log entry
        WorkflowUtils.newEntry(doc, session.getPrincipal().getName(),
                "Workflow upgraded", WorkflowExtConstants.WORKFLOW_CATEGORY,
                "Workflow upgraded from " + currentRouteVersion + " to " + workflowVersion, null, null);
        // Add Workflow audit line about upgrade
        DocumentEventContext envContext = new DocumentEventContext(session, session.getPrincipal(), doc);
        Map<String, Serializable> eventProperties = new HashMap<>();
        eventProperties.put("lastRoute", lastVersionRoute);
        eventProperties.put("newRoute", route);
        envContext.setProperties(eventProperties);
        EventProducer eventProducer = Framework.getLocalService(EventProducer.class);
        eventProducer.fireEvent(envContext.newEvent(RoutingConstants.UPGRADE_EVENT_NAME));
    }

    /**
     * Get last version route deployed.
     *
     * @param route
     * @param session
     * @return
     */
    public DocumentRoute getLastVersionRoutedDeployed(DocumentRoute route, CoreSession session) {
        // Get current version of route
        String routeVersion = WorkflowUtils.getRouteVersion(route);
        // Get title
        String workflowTitle = route.getDocument().getTitle();
        // Getting last version
        DocumentRoutingService routingService = Framework.getService(DocumentRoutingService.class);
        List<DocumentRoute> routesForDoc = routingService
                .getAvailableDocumentRoute(session);
        if (routesForDoc.size() == 1) {
            return route;
        }
        for (DocumentRoute routeForDoc : routesForDoc) {
            if (routeForDoc.getDocument().getTitle().equals(workflowTitle)) {
                String routeForDocVersion = WorkflowUtils.getRouteVersion(routeForDoc);
                if (WorkflowUtils.compareVersion(routeForDocVersion, routeVersion) > 0) {
                    return routeForDoc;
                }
            }
        }
        return route;
    }

    /**
     * Get document route for document given his model id.
     *
     * @param doc
     * @param modelId
     * @param session
     * @return
     */
    @Override
    public DocumentRoute getDocumentRoute(DocumentModel doc, String modelId, CoreSession session) {
        DocumentRoutingService documentRoutingService = getDocumentRoutingService();
        List<DocumentRoute> docRoutes = documentRoutingService.getDocumentRoutesForAttachedDocument(session, doc.getId());
        for (DocumentRoute route : docRoutes) {
            String routeModelId = route.getModelId();
            if (routeModelId.equals(modelId)) {
                return route;
            }
        }
        return null;
    }

    /**
     * Get document routes.
     *
     * @param doc
     * @param modelTitle
     * @param session
     * @return
     */
    @Override
    public List<DocumentRoute> getDocumentRoutes(DocumentModel doc, String modelTitle, CoreSession session) {
        DocumentRoutingService documentRoutingService = getDocumentRoutingService();
        List<DocumentRoute> routes = new ArrayList<>();
        List<DocumentRoute> docRoutes = documentRoutingService.getDocumentRoutesForAttachedDocument(session, doc.getId());
        for (DocumentRoute route : docRoutes) {
            String routeTitle = route.getTitle();
            if (routeTitle.equals(modelTitle)) {
                routes.add(route);
            }
        }
        return routes;
    }

    /**
     * Synchronize route with open tasks.
     *
     * @param openTasks
     * @param route
     */
    private void synchronizeOpenTask(List<Task> openTasks, DocumentRoute route, DocumentModel doc, CoreSession session) {
        TaskService taskService = getTaskService();
        DocumentRoutingService documentRoutingService = getDocumentRoutingService();
        // Cancel current open tasks of new route (normally it is the first task)
        List<Task> currentOpenTasks = getOpenTask(route, doc, session);
        for (Task task : currentOpenTasks) {
            if (task.isOpened()) {
                documentRoutingService.cancelTask(session, task.getDocument().getId());
                // Set node as ready
                GraphNode step = getTaskNode(route, task.getName(), session);
                step.setState(GraphNode.State.READY);
            }
        }
        GraphRoute graphRoute = route.getDocument().getAdapter(GraphRoute.class);
        // Open tasks in new route
        for (Task openTask : openTasks) {
            GraphNode taskNode = getTaskNode(route, openTask.getName(), session);
            if (taskNode == null) {
                LOG.debug("Task " + openTask.getName()
                        + " is not found to upgrade the route. Find upgrade information about this task.");
                List<GraphNode> upgradeRelatedNodes = findNodesWithUpgradeInfo(route, openTask);
                for (GraphNode upgradeNode : upgradeRelatedNodes) {
                    LOG.debug("Node located from openFrom " + upgradeNode.getId());
                    // This node must be open as new task
                    createTask(session, graphRoute, upgradeNode);
                    // Set node as suspended
                    upgradeNode.setState(GraphNode.State.SUSPENDED);
                }
                continue;
            }
            List<String> actors = openTask.getActors();
            if (actors.isEmpty()) {
                LOG.warn("No actors found in task " + openTask.getName() + ", " + openTask.getDocument().getId());
            }
            // Creating task in new route
            createTask(session, graphRoute, taskNode);
            // Set node as suspended
            taskNode.setState(GraphNode.State.SUSPENDED);
        }
    }

    /**
     * Find nodes for a task with upgrade info.
     *
     * @param route
     * @param task
     * @return
     */
    private List<GraphNode> findNodesWithUpgradeInfo(DocumentRoute route, Task task) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Find nodes with upgrade info for " + task.getName());
        }
        List<GraphNode> nodes = new ArrayList<>();
        GraphRoute graphRoute = route.getDocument().getAdapter(GraphRoute.class);
        for (GraphNode node : graphRoute.getNodes()) {
            if (!node.hasTask() && !node.hasMultipleTasks()) {
                continue;
            }
            String nodeSchema = "var-" + node.getId();
            if (node.getDocument().hasSchema(nodeSchema)) {
                try {
                    // Get upgradeInfo for each node
                    String upgradeInfo = (String) node.getDocument().getPropertyValue("var-" + node.getId() + ":upgradeInfo");
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Upgrade info " + upgradeInfo);
                    }
                    if (upgradeInfo != null && !upgradeInfo.isEmpty()) {
                        UpgradeInfo upgradeInfoForNode = new UpgradeInfo(upgradeInfo);
                        for (String openFromNode : upgradeInfoForNode.getOpenFrom()) {
                            if (task.getName().equals(openFromNode)) {
                                nodes.add(node);
                            }
                        }
                    }
                } catch (PropertyNotFoundException e) {
                    // No used
                }
            } else {
                LOG.warn("Node " + node.getId() + " has no var- schema");
            }
        }
        return nodes;
    }

    /**
     * Create task.
     *
     * @param session
     * @param graph
     * @param node
     * @throws DocumentRouteException
     */
    private void createTask(CoreSession session, GraphRoute graph, GraphNode node) throws DocumentRouteException {
        DocumentRouteElement routeInstance = graph;
        Map<String, String> taskVariables = new HashMap<>();
        taskVariables.put(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY,
                routeInstance.getDocument().getId());
        taskVariables.put(DocumentRoutingConstants.TASK_NODE_ID_KEY, node.getId());
        taskVariables.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY, node.getDocument().getId());
        String taskNotiftemplate = node.getTaskNotificationTemplate();
        if (!StringUtils.isEmpty(taskNotiftemplate)) {
            taskVariables.put(DocumentRoutingConstants.TASK_ASSIGNED_NOTIFICATION_TEMPLATE, taskNotiftemplate);
        } else {
            // disable notification service
            taskVariables.put(TaskEventNames.DISABLE_NOTIFICATION_SERVICE, "true");
            taskVariables.put(RoutingConstants.TASK_IGNORE_ASSIGNMENT_NOTIFICATION, "true");
        }
        // evaluate task assignees from taskVar if any
        HashSet<String> actors = new LinkedHashSet<String>();
        actors.addAll(node.evaluateTaskAssignees());
        actors.addAll(node.getTaskAssignees());
        // evaluate taskDueDate from the taskDueDateExpr;
        Date dueDate = node.computeTaskDueDate();
        DocumentModelList docs = graph.getAttachedDocumentModels();
        TaskService taskService = Framework.getService(TaskService.class);
        DocumentRoutingService routing = Framework.getService(DocumentRoutingService.class);
        List<Task> tasks = taskService.createTask(session, (NuxeoPrincipal) session.getPrincipal(), docs,
                node.getTaskDocType(), node.getDocument().getTitle(), node.getId(), routeInstance.getDocument().getId(),
                new ArrayList<>(actors), node.hasMultipleTasks(), node.getTaskDirective(), null, dueDate,
                taskVariables, null, node.getWorkflowContextualInfo(session, true));

        // Audit task assignment
        for (Task task : tasks) {
            Map<String, Serializable> eventProperties = new HashMap<>();
            eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY, DocumentRoutingConstants.ROUTING_CATEGORY);
            eventProperties.put("taskName", node.getDocument().getTitle());
            eventProperties.put("actors", actors);
            eventProperties.put("modelId", graph.getModelId());
            eventProperties.put("modelName", graph.getModelName());
            eventProperties.put(RoutingAuditHelper.WORKFLOW_INITATIOR, graph.getInitiator());
            eventProperties.put(RoutingAuditHelper.TASK_ACTOR, ((NuxeoPrincipal) session.getPrincipal()).getOriginatingUser());
            eventProperties.put("nodeVariables", (Serializable) node.getVariables());
            if (routeInstance instanceof GraphRoute) {
                eventProperties.put("workflowVariables", (Serializable) ((GraphRoute) routeInstance).getVariables());
            }

            // compute duration since workflow started
            long timeSinceWfStarted = RoutingAuditHelper.computeDurationSinceWfStarted(task.getProcessId());
            if (timeSinceWfStarted >= 0) {
                eventProperties.put(RoutingAuditHelper.TIME_SINCE_WF_STARTED, timeSinceWfStarted);
            }

            DocumentEventContext envContext = new DocumentEventContext(session, session.getPrincipal(), task.getDocument());
            envContext.setProperties(eventProperties);
            EventProducer eventProducer = Framework.getService(EventProducer.class);
            eventProducer.fireEvent(envContext.newEvent(DocumentRoutingConstants.Events.afterWorkflowTaskCreated.name()));
        }
        for (Task task : tasks) {
            node.addTaskInfo(task.getId());
        }
        String taskAssigneesPermission = node.getTaskAssigneesPermission();
        if (StringUtils.isEmpty(taskAssigneesPermission)) {
            return;
        }
        for (Task task : tasks) {
            routing.grantPermissionToTaskAssignees(session, taskAssigneesPermission, docs, task);
        }
    }

    /**
     * Get task node.
     *
     * @param route
     * @param nodeName
     * @param session
     * @return
     */
    private GraphNode getTaskNode(DocumentRoute route, String nodeName, CoreSession session) {
        DocumentModel workflowInstance = session.getDocument(new IdRef(route.getDocument().getId()));
        GraphRoute workflowGraph = workflowInstance.getAdapter(GraphRoute.class);
        try {
            return workflowGraph.getNode(nodeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get open tasks for route.
     *
     * @param route
     * @param doc
     * @return
     */
    private List<Task> getOpenTask(DocumentRoute route, DocumentModel doc, CoreSession session) {
        TaskService taskService = getTaskService();
        List<Task> tasks = new ArrayList<>();
        List<Task> openTasks = taskService.getAllTaskInstances(route.getDocument().getId(), session);
        for (Task task : openTasks) {
            if (task.getTargetDocumentsIds().contains(doc.getId())) {
                if ("opened".equals(task.getDocument().getCurrentLifeCycleState())) {
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    /**
     * Close open task for route.
     *
     * @param route
     * @param doc
     * @param session
     */
    private void closeOpenTask(DocumentRoute route, DocumentModel doc, CoreSession session) {
        TaskService taskService = getTaskService();
        List<Task> openTasks = taskService.getAllTaskInstances(route.getDocument().getId(), session);
        for (Task task : openTasks) {
            if (task.getTargetDocumentsIds().contains(doc.getId())) {
                if ("opened".equals(task.getDocument().getCurrentLifeCycleState())) {
                    LOG.info("Canceling task " + task.getId() + " for route " + route.getDocument().getId());
                    task.cancel(session);
                }
            }
        }
    }

    /**
     * Copy workflow vars.
     *
     * @param originRoute
     * @param destinyRoute
     */
    private void copyWorkflowVars(DocumentRoute originRoute, DocumentRoute destinyRoute, String workflowTitle) throws UpgradeWorkflowException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Copying workflow vars from " + originRoute.getDocument().getId() + " to " + destinyRoute.getDocument().getId());
        }
        DocumentModel originDoc = originRoute.getDocument();
        DocumentModel destinyDoc = destinyRoute.getDocument();
        String originSchema = getSchemaForRoute(originRoute, workflowTitle);
        if (originSchema == null) {
            throw new UpgradeWorkflowException("Not found schema for origin route with title " + workflowTitle);
        }
        String destinySchema = getSchemaForRoute(destinyRoute, workflowTitle);
        if (destinySchema == null) {
            throw new UpgradeWorkflowException("Not found schema for destiny route with title " + workflowTitle);
        }
        LOG.info(originSchema + " => " + destinySchema);
        // The schema name is the same as workflowInstance title => copy properties to schema from origin to destiny
        Map<String, Object> properties = originDoc.getProperties(originSchema);
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String metadata = entry.getKey();
            if (metadata.contains(":")) {
                metadata = metadata.split(":")[1];
            }
            if (!RoutingConstants.isCoreRoutingProperty(metadata)) {
                if (entry.getValue() instanceof  Serializable) {
                    destinyDoc.setPropertyValue(destinySchema + ":" + metadata, (Serializable) entry.getValue());
                }
            }
        }
        LOG.info("Destiny doc properties " + destinyDoc.getProperties(destinySchema));
    }

    /**
     * Get schema for a route document.
     *
     * @param route
     * @param schemaName
     * @return
     */
    private String getSchemaForRoute(DocumentRoute route, String schemaName) {
        String [] schemas = route.getDocument().getSchemas();
        for (String schema : schemas) {
            if (schema.startsWith("wf-" + schemaName)) {
                return schema;
            }
        }
        return null;
    }

    private TaskService getTaskService() {
        return Framework.getService(TaskService.class);
    }

    private DocumentRoutingService getDocumentRoutingService() {
        return Framework.getService(DocumentRoutingService.class);
    }
}
