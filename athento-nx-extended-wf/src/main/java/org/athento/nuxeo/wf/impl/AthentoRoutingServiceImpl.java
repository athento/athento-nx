package org.athento.nuxeo.wf.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.wf.api.AthentoRoutingService;
import org.athento.nuxeo.wf.api.RoutingConstants;
import org.athento.nuxeo.wf.exception.UpgradeWorkflowException;
import org.athento.nuxeo.wf.utils.WorkflowUtils;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
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
        if (lastVersionRoute == null) {
            throw new UpgradeWorkflowException("There is no version to upgrade for route " + route.getTitle());
        }
        String workflowTitle = route.getDocument().getTitle();
        LOG.info("Last route deployed for " + workflowTitle + " is " + lastVersionRoute.getDocument().getId()
                + " with version " + WorkflowUtils.getRouteVersion(lastVersionRoute));
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
        // Synchronize open task into current route
        synchronizeOpenTask(openLastTasks, newRoute, doc, session);
        // Close open task for current route
        closeOpenTask(route, doc, session);
    }

    /**
     * Get last version route deployed.
     *
     * @param route
     * @param session
     * @return
     */
    private DocumentRoute getLastVersionRoutedDeployed(DocumentRoute route, CoreSession session) {
        LOG.info("Getting last version route deployed...");
        // Get current version of route
        String routeVersion = WorkflowUtils.getRouteVersion(route);
        // Get title
        String workflowTitle = route.getDocument().getTitle();
        // Getting last version
        DocumentRoutingService routingService = Framework.getService(DocumentRoutingService.class);
        List<DocumentRoute> routesForDoc = routingService
                .getAvailableDocumentRoute(session);
        for (DocumentRoute routeForDoc : routesForDoc) {
            if (routeForDoc.getDocument().getTitle().equals(workflowTitle)) {
                String routeForDocVersion = WorkflowUtils.getRouteVersion(routeForDoc);
                if (WorkflowUtils.compareVersion(routeForDocVersion, routeVersion) > 0) {
                    return routeForDoc;
                }
            }
        }
        return null;
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
     * Synchronize route with open tasks.
     *
     * @param openTasks
     * @param route
     */
    private void synchronizeOpenTask(List<Task> openTasks, DocumentRoute route, DocumentModel doc, CoreSession session) {
        TaskService taskService = getTaskService();
        DocumentRoutingService documentRoutingService = getDocumentRoutingService();
        NuxeoPrincipal pral = (NuxeoPrincipal) session.getPrincipal();
        for (Task openTask : openTasks) {
            Task currentRouteTask = taskService.getTask(session, openTask.getId());
            if (currentRouteTask == null) {
                LOG.info("Task " + openTask.getName()
                        + " is not found to upgrade the route");
                continue;
            }
            List<String> actors = openTask.getActors();
            if (actors.isEmpty()) {
                LOG.info("No actors found in task " + openTask.getName() + ", " + openTask.getDocument().getId());
                continue;
            }
            // Find openTask node in to currentRoute to synchronize
            GraphNode taskNode = getTaskNode(route, openTask.getName(), session);
            if (taskNode != null) {
                // Cancel current open tasks of new route
                List<Task> currentOpenTasks = getOpenTask(route, doc, session);
                for (Task task : currentOpenTasks) {
                    if (task.isOpened()) {
                        documentRoutingService.cancelTask(session, task.getDocument().getId());
                    }
                }
                LOG.info("Task Node " + taskNode.getDocument().getCurrentLifeCycleState());
                Map<String, String> taskVariables = new HashMap<String, String>();
                taskVariables.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY, taskNode.getDocument().getId());
                // disable notification service
                taskVariables.put(TaskEventNames.DISABLE_NOTIFICATION_SERVICE, "true");
                taskVariables.put(RoutingConstants.TASK_IGNORE_ASSIGNMENT_NOTIFICATION, "true");
                LOG.info("Task to open " + taskNode.getId());
                List<Task> tasks = taskService.createTask(session, pral, doc, taskNode.getId(),
                        actors, false, taskNode.getTaskDirective(), null, taskNode.getTaskDueDate(), taskVariables, null);
                DocumentRoutingService routing = Framework.getService(DocumentRoutingService.class);
                routing.makeRoutingTasks(session, tasks);
                for (Task task : tasks) {
                    DocumentRouteStep step = (DocumentRouteStep) getTaskNode(route, task.getName(), session);
                    LOG.info("Stemp " + step + " for task " + task);
                    // all the actors should be able to validate the step creating the task
                    for (String actor : actors) {
                        step.setCanReadStep(session, actor);
                        step.setCanValidateStep(session, actor);
                        step.setCanUpdateStep(session, actor);
                    }
                }
            }
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
        return workflowGraph.getNode(nodeName);
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
