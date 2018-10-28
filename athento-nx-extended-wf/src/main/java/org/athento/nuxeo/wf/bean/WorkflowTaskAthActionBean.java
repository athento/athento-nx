package org.athento.nuxeo.wf.bean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.wf.utils.WorkflowUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskEventNames;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.task.core.helpers.TaskActorsHelper;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItem;
import org.nuxeo.ecm.platform.task.dashboard.DashBoardItemImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Task action bean.
 */
@Name("athWorkflowTaskActionBean")
@Scope(ScopeType.CONVERSATION)
public class WorkflowTaskAthActionBean implements Serializable {

    private Log LOG = LogFactory.getLog(WorkflowTaskAthActionBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected WebActions webActions;

    @In(create = true)
    protected transient TaskService taskService;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    protected List<Task> tasks;

    protected List<DashBoardItem> items;

    /**
     * Cancel the workflow.
     */
    public String cancelRoute(DocumentRoute route) {
        LOG.info("Canceling route " + route);
        Framework.getLocalService(DocumentRoutingEngineService.class).cancel(route, documentManager);
        // force computing of tabs
        webActions.resetTabList();
        Events.instance().raiseEvent(TaskEventNames.WORKFLOW_CANCELED);
        Contexts.removeFromAllContexts("relatedRoutes");
        documentManager.save();
        return navigationContext.navigateToDocument(navigationContext.getCurrentDocument());
    }

    /**
     * Get related routes for a document.
     *
     * @return
     */
    public List<String> getRelatedRoutesDocIds() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        List<DocumentModel> relatedRoutes = findRelatedRoute(currentDocument.getId());
        List<String> routesIds = new ArrayList<String>(relatedRoutes.size());
        for (DocumentModel route : relatedRoutes) {
            routesIds.add(route.getId());
        }
        return routesIds;
    }

    /**
     * Get route instance from task id.
     *
     * @param taskId
     * @return
     */
    public DocumentModel getRouteInstanceFor(String taskId) {
        DocumentModel taskDoc = documentManager.getDocument(new IdRef(taskId));
        Task task = taskDoc.getAdapter(Task.class);
        final String routeDocId = task.getVariable(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY);
        if (routeDocId == null) {
            return null;
        }
        final DocumentModel[] res = new DocumentModel[1];
        new UnrestrictedSessionRunner(documentManager) {
            @Override
            public void run() {
                DocumentModel doc = session.getDocument(new IdRef(routeDocId));
                doc.detach(true);
                res[0] = doc;
            }
        }.runUnrestricted();
        return res[0];
    }

    /**
     * Find related routes for a document.
     *
     * @param documentId
     * @return
     */
    public List<DocumentModel> findRelatedRoute(String documentId) {
        List<DocumentModel> docs = new ArrayList<DocumentModel>();
        if (documentId == null || "".equals(documentId)) {
            return docs;
        }
        List<DocumentRoute> relatedRoutes = Framework.getService(DocumentRoutingService.class).getDocumentRoutesForAttachedDocument(
                documentManager, documentId);
        for (DocumentRoute documentRoute : relatedRoutes) {
            docs.add(documentRoute.getDocument());
        }
        return docs;
    }

    /**
     * Modify from NX to get task for any route of document.
     *
     * @param route
     * @return
     */
    public List<Task> getCurrentRouteAllTasks(DocumentRoute route) {
        TaskService taskService = Framework.getLocalService(TaskService.class);
        if (route != null) {
            return taskService.getAllTaskInstances(route.getDocument().getId(), documentManager);
        }
        return null;
    }

    public List<Task> getCurrentDocumentTasks(DocumentRoute route) {
        tasks = new ArrayList<>();
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            NuxeoPrincipal principal = (NuxeoPrincipal) documentManager.getPrincipal();
            List<String> actors = new ArrayList<>();
            actors.addAll(TaskActorsHelper.getTaskActors(principal));
            List<Task> tempTasks = taskService.getTaskInstances(currentDocument, actors, true, documentManager);
            if (route != null) {
                // Check task for route
                for (Task task : tempTasks) {
                    DocumentModel routeModel = WorkflowUtils.getTaskRouteDocumentModel(task, documentManager);
                    DocumentRoute documentRoute = routeModel.getAdapter(DocumentRoute.class);
                    if (documentRoute.getModelName().equals(route.getModelName())) {
                        tasks.add(task);
                    }
                }
            }
        }
        return tasks;
    }

    public List<DashBoardItem> getCurrentDashBoardItemsExceptPublishingTasks(DocumentRoute route) {
        items = new ArrayList<>();
        for (Task task : getCurrentDocumentTasks(route)) {
            String taskType = task.getVariable(Task.TaskVariableName.taskType.name());
            if (!"publish_moderate".equals(taskType)) {
                DashBoardItem item = new DashBoardItemImpl(task, navigationContext.getCurrentDocument(),
                        localeSelector.getLocale());
                items.add(item);
            }
        }
        return items;
    }
}
