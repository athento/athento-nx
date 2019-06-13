package org.athento.nuxeo.wf.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.wf.utils.WorkflowUtils;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRouteImpl;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.runtime.api.Framework;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listener for task completed listener.
 */
public class TaskCompletedListener implements EventListener {

    private static final Log LOG = LogFactory
            .getLog(TaskCompletedListener.class);

    /**
     * Last transition execution control.
     */
    private static Map<String, String> lastExecutedTransition = new HashMap<>();

    /**
     * Handle notification.
     *
     * @param event
     */
    @Override
    public void handleEvent(Event event) {
        if (event != null) {
            if (!Framework.getRuntime().isStarted()) {
                return;
            }
            CoreSession session = event.getContext().getCoreSession();
            if (event.getName().equals(DocumentRoutingConstants.Events.beforeRouteStart.name())) {
                // Start task
                lastExecutedTransition.clear();
                GraphRouteImpl startRoute = (GraphRouteImpl) event.getContext().getProperty("documentElementEventContextKey");
                GraphNode startNode = startRoute.getStartNode();
                List<GraphNode.Transition> nodeTransitions = startNode.getOutputTransitions();
                if (nodeTransitions.size() > 0) {
                    String transitionExecuted = nodeTransitions.get(0).getId();
                    if (!startRoute.getAttachedDocumentModels().isEmpty()) {
                        executeTransition(session, startRoute.getAttachedDocumentModels().get(0), startNode, transitionExecuted);
                    }
                }
            } else if (event.getName().equals("documentModified")) {
                // Check transition after node document modification
                DocumentModel autoNode = ((DocumentEventContext) event.getContext()).getSourceDocument();
                if (autoNode.getType().equals("RouteNode")) {
                    GraphNode node = autoNode.getAdapter(GraphNode.class);
                    if (!node.hasTask()) {
                        DocumentModel startRouteDoc = session.getDocument(autoNode.getParentRef());
                        if (startRouteDoc != null) {
                            GraphRoute startRoute = startRouteDoc.getAdapter(GraphRoute.class);
                            // Check transitions
                            List<GraphNode.Transition> nodeTransitions = node.getOutputTransitions();
                            if (nodeTransitions.size() > 0) {
                                String transitionExecuted = nodeTransitions.get(0).getId();
                                if (!startRoute.getAttachedDocumentModels().isEmpty()) {
                                    executeTransition(session, startRoute.getAttachedDocumentModels().get(0), node, transitionExecuted);
                                }
                            }
                        }
                    }
                }
            } else {
                // Task completed
                // Add task property from event document
                DocumentModel document = ((DocumentEventContext) event.getContext()).getSourceDocument();
                if (document != null) {
                    Task task = WorkflowUtils.getTaskFromDocument(event.getContext());
                    if (task != null) {
                        DocumentModel taskNodeDoc = WorkflowUtils.getDocumentNodeFromTask(session, task);
                        // Check transitions
                        if (taskNodeDoc != null) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Task node doc to check transitions " + taskNodeDoc.getId() + ", " + taskNodeDoc.getName());
                            }
                            manageTransitions(session, task, taskNodeDoc);
                        }
                        // Create audit of task completed
                        String taskUsername = (String) task.getDocument().getPropertyValue("dc:lastContributor");
                        WorkflowUtils.createAuditChange(document, task, taskUsername,
                                true);
                    }
                }
            }
        }
    }

    /**
     * Manage lifecycle transitions.
     *
     * @param session
     * @param task
     * @param taskNodeDoc
     */
    private void manageTransitions(CoreSession session, Task task, DocumentModel taskNodeDoc) {
        String nodeId = (String) taskNodeDoc.getPropertyValue("rnode:nodeId");
        if (taskNodeDoc.hasSchema("var-" + nodeId)) {
            try {
                if (isAbleToManage(taskNodeDoc, "var-" + nodeId + ":lfTransitions")) {
                    String transitionsData = (String) taskNodeDoc.getPropertyValue("var-" + nodeId + ":lfTransitions");
                    if (transitionsData != null) {
                        DocumentModel targetDoc = session.getDocument(new IdRef(task.getTargetDocumentsIds().get(0)));
                        DocumentModel nextTaskDocument = WorkflowUtils.getTaskDocument(session, targetDoc, "opened");
                        if (nextTaskDocument != null) {
                            Task nextTask = nextTaskDocument.getAdapter(Task.class);
                            DocumentModel nextNodeDocument = WorkflowUtils.getDocumentNodeFromTask(session, nextTask);
                            if (nextNodeDocument != null) {
                                String transitionExecuted = getLastWorkflowTransitionExecuted(taskNodeDoc, (String) nextNodeDocument.getPropertyValue("rnode:nodeId"));
                                if (transitionExecuted != null) {
                                    GraphNode node = taskNodeDoc.getAdapter(GraphNode.class);
                                    executeTransition(session, WorkflowUtils.getTargetDocumentFromTask(session, nextTask), node, transitionExecuted);
                                }
                            }
                        }
                    }
                }
            } catch (PropertyException e) {
                LOG.warn("Property lfTransitions is not found for taskNode " + taskNodeDoc.getId());
            } catch (NuxeoException e) {
                LOG.warn("Follow transition in task complete has an error", e);
            }
        }
    }

    /**
     * Check if a task is able to manage of Athento Extended Workflow.
     *
     * @param taskNode
     * @param property
     * @return
     */
    private boolean isAbleToManage(DocumentModel taskNode, String property) {
        try {
            taskNode.getProperty(property);
            return true;
        } catch (PropertyNotFoundException e) {
            return false;
        }
    }

    /**
     * Execute transition.
     *
     * @param session
     * @param targetDocument
     * @param node
     * @param transitionExecuted
     */
    private void executeTransition(CoreSession session, DocumentModel targetDocument, GraphNode node, String transitionExecuted) {
        try {
            if (transitionExecuted == null) {
                return;
            }
            String transitionsData = (String) node.getDocument().getPropertyValue("var-" + node.getId() + ":lfTransitions");
            String[] transitions = transitionsData.split(";");
            for (String transitionInfo : transitions) {
                String[] transitionData = transitionInfo.split(":");
                if (transitionData.length == 2) {
                    String transitionName = transitionData[0];
                    if (transitionExecuted.equals(transitionName)) {
                        String transition = transitionData[1];
                        if (transition != null && !"null".equals(transition)
                                && !isLastExecutedTransition(targetDocument.getId(), transition)) {
                            LOG.info("Executing transition " + transition);
                            session.followTransition(targetDocument, transition);
                            // Update lastExecutedTransition
                            lastExecutedTransition.put(targetDocument.getId(), transition);
                            return;
                        }
                    }
                }
            }
        } catch (PropertyNotFoundException e) {
            LOG.trace("No control for node with transitions");
        }
    }

    /**
     * Check if transition is the last executed.
     *
     * @param id
     * @param transition
     * @return
     */
    private boolean isLastExecutedTransition(String id, String transition) {
        String lastTransition = lastExecutedTransition.get(id);
        return lastTransition != null && lastTransition.equals(transition);
    }

    /**
     * Get last transition executed.
     *
     * @param lastNode
     * @param currentNodeId
     * @return
     */
    private String getLastWorkflowTransitionExecuted(DocumentModel lastNode, String currentNodeId) {
        if (lastNode == null) {
            return null;
        }
        List<Map<String, Object>> transitions = (List<Map<String, Object>>) lastNode.getPropertyValue("rnode:transitions");
        for (Map<String, Object> transition : transitions) {
            String targetId = (String) transition.get("targetId");
            if (targetId != null && targetId.equals(currentNodeId)) {
                return (String) transition.get("name");
            }
        }
        return null;

    }


}
