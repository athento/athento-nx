package org.athento.nuxeo.wf.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.actions.jsf.JSFActionContext;
import org.nuxeo.ecm.platform.actions.seam.SeamActionContext;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskComment;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;

import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.*;

/**
 * Created by victorsanchez on 24/11/16.
 */
public final class WorkflowUtils {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(WorkflowUtils.class);

    /** Extended config path. */
    public static final String CONFIG_PATH = "/ExtendedConfig";

    /**
     * Token APP.
     */
    private static final String APP_NAME = "athentoWorkflowExt";


    /**
     * Read config value.
     *
     * @param session
     * @param key
     * @return
     */
    public static <T> T readConfigValue(CoreSession session, final String key, final Class<T> clazz) {
        final List<T> value = new ArrayList<T>(1);
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() throws ClientException {
                DocumentModel conf = session.getDocument(new PathRef(
                        WorkflowUtils.CONFIG_PATH));
                value.add(clazz.cast(conf.getPropertyValue(key)));
            }
        }.runUnrestricted();
        if (value.isEmpty()) {
            return null;
        }
        return value.get(0);
    }

    /**
     * Run an operation.
     *
     * @param operationId
     * @param input
     * @param params
     * @param session
     * @return
     * @throws Exception
     */
    public static Object runOperation(String operationId, Object input,
                                      Map<String, Object> params, CoreSession session)
            throws Exception {
        AutomationService automationManager = Framework
                .getLocalService(AutomationService.class);
        // Input setting
        OperationContext ctx = new OperationContext(session);
        ctx.putAll(params);
        ctx.setInput(input);
        return automationManager.run(ctx, operationId, params);
    }

    /**
     * Get task from source document.
     *
     * @param ctxt
     * @return task
     */
    public static Task getTaskFromDocument(EventContext ctxt) {
        return (Task) ctxt.getProperties().get("taskInstance");
    }

    /**
     * Get task id from source document.
     *
     * @param ctxt
     * @return
     */
    public static String getTaskIdFromDocument(EventContext ctxt) {
        Task task = (Task) ctxt.getProperties().get("taskInstance");
        if (task == null) {
            return null;
        }
        return task.getDocument().getId();
    }


    /**
     * Get from source document.
     *
     * @param ctxt
     * @return
     */
    public static String getNodeIdFromDocument(EventContext ctxt) {
        String nodeId = (String) ctxt.getProperties().get("nodeId");
        return nodeId;
    }

    /**
     * Get task document.
     *
     * @param session is the session
     * @param doc is the document
     * @param state is the state of task
     * @return
     */
    public static DocumentModel getTaskDocument(CoreSession session, DocumentModel doc, String state) {
        DocumentModelList tasks =
                session.query("SELECT * FROM TaskDoc WHERE nt:targetDocumentId = '" + doc.getId() + "' AND ecm:currentLifeCycleState = '" + state + "' ORDER BY dc:modified DESC");
        if (!tasks.isEmpty()) {
            return tasks.get(0);
        }
        return null;
    }

    /**
     * Get tasks document with state.
     *
     * @param session is the session
     * @param doc is the document
     * @param state is the state of task
     * @return
     */
    public static DocumentModelList getTaskDocuments(CoreSession session, DocumentModel doc, String state) {
        DocumentModelList tasks =
                session.query("SELECT * FROM TaskDoc WHERE nt:targetDocumentId = '" + doc.getId() + "' AND ecm:currentLifeCycleState = '" + state + "' ORDER BY dc:modified DESC");
        return tasks;
    }

    /**
     * Generate a simple preview token based on dublincore:modified metadata.
     *
     * @param doc document
     * @return token
     */
    public static String generatePreviewToken(DocumentModel doc) {
        // Encoding token
        return Base64.encodeBase64String(String.format("%s#control", doc.getChangeToken()).getBytes());
    }

    /**
     * Check for document content.
     *
     * @param document
     * @return
     */
    public static boolean hasContent(DocumentModel document) {
        if (document.hasSchema("file")) {
            return document.getPropertyValue("file:content") != null;
        }
        return false;
    }

    /**
     * Generate access token to document based en Nuxeo Token Auth.
     *
     * @param principals
     * @return generated access token
     */
    public static HashMap<String, String> generateAccessTokensAuth(String[] principals) {
        HashMap<String, String> tokens = new HashMap<>();
        TokenAuthenticationService tokenAuthService = Framework.getService(TokenAuthenticationService.class);
        for (String principal : principals) {
            tokens.put(principal, tokenAuthService.acquireToken(principal, APP_NAME, "default", "default", "rw"));
        }
        return tokens;
    }

    /**
     * Get task principals.
     *
     * @param ctxt
     * @return
     */
    public static String[] getTaskPrincipals(EventContext ctxt) {
        List<String> actorResult = new ArrayList<String>();
        Task task = (Task) ctxt.getProperties().get("taskInstance");
        UserManager userManager = Framework.getService(UserManager.class);
        List<String> actors = task.getActors();
        for (String actor : actors) {
            if (actor.startsWith("group:")) {
                NuxeoGroup group = userManager.getGroup(actor.split(":")[1]);
                for (String user : group.getMemberUsers()) {
                    actorResult.add(user);
                }
            } else if (actor.startsWith("user:")) {
                actorResult.add(actor.split(":")[1]);
            } else {
                actorResult.add(actor);
            }
        }
        return actorResult.toArray(new String[0]);
    }

    /**
     * Init bindings.
     *
     * @param map
     * @param session
     * @param doc
     */
    public static void initBindings(Map<String, Serializable> map, CoreSession session, DocumentModel doc) {
        map.put("This", doc);
        map.put("docTitle", doc.getTitle());
        map.put("CurrentUser", (NuxeoPrincipal) session.getPrincipal());
        map.put("currentUser", (NuxeoPrincipal) session.getPrincipal());
        map.put("Env", Framework.getProperties());
        map.put("Fn", new Functions());
    }

    /**
     * Get string from a Set.
     *
     * @param set
     * @return
     */
    public static String getStringFromSet(Set<String> set) {
        StringBuffer userBuff = new StringBuffer();
        for (Iterator<String> it = set.iterator(); it.hasNext();) {
            userBuff.append(it.next() + (it.hasNext() ? ", " : ""));
        }
        return userBuff.toString();
    }

    /**
     * Get task information.
     *
     * @param session
     * @param task
     * @param getFormVariables
     * @return
     * @throws ClientException
     */
    public static Map<String, Serializable> getTaskInfo(final CoreSession session, final Task task, final boolean getFormVariables)
            throws ClientException {
        final String routeDocId = task.getVariable(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY);
        final String nodeId = task.getVariable(DocumentRoutingConstants.TASK_NODE_ID_KEY);
        if (routeDocId == null) {
            throw new ClientException(
                    "Can not get the source graph for this task");
        }
        if (nodeId == null) {
            throw new ClientException(
                    "Can not get the source node for this task");
        }
        final HashMap<String, Serializable> map = new HashMap<String, Serializable>();
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() throws ClientException {
                DocumentModel doc = session.getDocument(new IdRef(routeDocId));
                GraphRoute route = doc.getAdapter(GraphRoute.class);
                GraphNode node = route.getNode(nodeId);
                if (getFormVariables) {
                    map.putAll(node.getVariables());
                    map.putAll(route.getVariables());
                }
            }
        }.runUnrestricted();
        return map;
    }

    /**
     * Notify users for task. Based on the property notifyUsers.
     *
     * @param session
     * @param task
     * @param document
     * @param properties
     */
    public static void notifiyUsers(CoreSession session, Task task, DocumentModel document, Map<String, Serializable> properties) {
        if (task != null) {
            String [] notifyUsers = (String []) properties.get("notifyUsers");
            if (notifyUsers == null) {
                return;
            }
            for (String notifyUser : notifyUsers) {
                if (notifyUser != null && !notifyUser.isEmpty()) {
                    sendEmailNotification(session, task, document, notifyUser, properties);
                }
            }
        }
    }

    /**
     * Send email notification.
     *
     * @param session
     * @param task
     * @param document
     * @param userOrGroup
     * @param properties
     */
    public static void sendEmailNotification(CoreSession session, Task task, DocumentModel document, String userOrGroup, Map<String, Serializable> properties) {
        try {
            if (userOrGroup == null || userOrGroup.isEmpty()) {
                LOG.info("No destination user or group defined in notification");
                return;
            }
            Map<String, Object> params = new HashMap<>();
            params.putAll(properties);
            params.put("taskId", task.getId());
            params.put("toUser", userOrGroup.trim());
            if (LOG.isInfoEnabled()) {
                LOG.info("Sending email notification toUser: " + userOrGroup.trim());
            }
            String template = task.getVariable("taskNotificationTemplate");
            if (template == null || "template:workflowTaskAssigned".equals(template) || "workflowTaskAssigned".equals(template)) {
                template = "workflowTaskAssignedGeneric";
            }
            params.put("template", "template:" + template);
            if (LOG.isInfoEnabled()) {
                LOG.info("Sending email notification with template: " + template);
            }
            params.put("subject", "[Athento] Task assigned " + document.getName());
            params.put("html", true);
            boolean includeAttachments = Boolean.valueOf(Framework.getProperty("athento.workflow.notification.includeattachments",
                                                                               "true"));
            if (includeAttachments) {
                // Add blobs it document has whether it is a valid attachment
                StringList attachments = new StringList();
                if (document.hasSchema("file")) {
                    if (isValidAttachment(document, "file:content")) {
                        attachments.add("file:content");
                    }
                }
                if (document.hasSchema("files")) {
                    ListProperty files = (ListProperty) document.getProperty("files:files");
                    for (int i = 0; i < files.size(); i++) {
                        String xpath = "files:files/file[" + i + "]/file";
                        if (isValidAttachment(document, xpath)) {
                            attachments.add(xpath);
                        }

                    }
                }
                if (!attachments.isEmpty()) {
                    params.put("files", attachments);
                }
            }
            if (LOG.isInfoEnabled()) {
                LOG.info("Sending email notification. Executing operation.");
            }
            WorkflowUtils.runOperation("Athento.SendNotificationTaskAssigned", document, params, session);
        } catch (Exception e) {
            LOG.error("Error sending notification", e);
        }
    }

    /**
     * Check if attachment is valid for email notification.
     * @param doc
     * @param xpath
     * @return
     */
    private static boolean isValidAttachment(DocumentModel doc, String xpath) {
        try {
            ArrayList<Blob> blobs = new ArrayList<>();
            Property p = doc.getProperty(xpath);
            if (p instanceof BlobProperty) {
                getAttachmentBlob(p.getValue(), blobs);
            } else if (p instanceof ListProperty) {
                for (Property pp : p) {
                    getAttachmentBlob(pp.getValue(), blobs);
                }
            } else if (p instanceof MapProperty) {
                for (Property sp : ((MapProperty) p).values()) {
                    getAttachmentBlob(sp.getValue(), blobs);
                }
            } else {
                Object o = p.getValue();
                if (o instanceof Blob) {
                    if (((Blob) o).getLength() <= WorkflowExtConstants.MAX_ATTACHMENT_SIZE) {
                        blobs.add((Blob) o);
                    }
                }
            }
            return blobs.size() > 0;
        } catch (org.nuxeo.ecm.core.api.PropertyException e) {
            LOG.error("Property error checking valid attachment", e);
        }
        return false;
    }

    /**
     * Get blob for attachments.
     * @param o
     * @param blobs
     */
    private static void getAttachmentBlob(Object o, List<Blob> blobs) {
        if (o instanceof List) {
            for (Object item : (List<Object>) o) {
                getAttachmentBlob(item, blobs);
            }
        } else if (o instanceof Map) {
            for (Object item : ((Map<String, Object>) o).values()) {
                getAttachmentBlob(item, blobs);
            }
        } else if (o instanceof Blob) {
            if (((Blob) o).getLength() <= WorkflowExtConstants.MAX_ATTACHMENT_SIZE) {
                blobs.add((Blob) o);
            }
        }
    }

    /**
     * New log entry.
     *
     * @param doc
     * @param principal
     * @param event
     * @param category
     * @param comment
     * @param directive
     * @return
     */
    public static LogEntry newEntry(DocumentModel doc, String principal, String event, String category, String comment, ExtendedInfo directive, ExtendedInfo dueDate) {
        AuditLogger auditLogger = Framework.getService(AuditLogger.class);
        LogEntry entry = auditLogger.newLogEntry();
        entry.setEventId(event);
        entry.setEventDate(new Date());
        entry.setCategory(category);
        entry.setDocUUID(doc.getId());
        entry.setDocPath(doc.getPathAsString());
        entry.setComment(comment);
        entry.setPrincipalName(principal);
        entry.setDocType(doc.getType());
        entry.setRepositoryId(doc.getRepositoryName());
        if (directive != null) {
            entry.getExtendedInfos().put("directive", directive);
        }
        if (dueDate != null){
            entry.getExtendedInfos().put("dueDate", dueDate);
        }
        try {
            entry.setDocLifeCycle(doc.getCurrentLifeCycleState());
        } catch (Exception e) {
            // ignore error
        }
        auditLogger.addLogEntries(Collections.singletonList(entry));
        return entry;
    }

    /**
     * Create audit change.
     */
    public static void createAuditChange(DocumentModel document, Task task, String user) {
        createAuditChange(document, task, user, false);
    }

    /**
     * Create audit change.
     */
    public static void createAuditChange(DocumentModel document, Task task, String user, boolean taskCompleted) {
        String taskName = task.getType();
        String comment = "No comment";
        List<TaskComment> comments = task.getComments();
        if (comments == null || comments.isEmpty()) {
            if (taskCompleted) {
                comment = "Task completed by " + user;
            } else {
                comment = "Workflow changed by " + user;
            }
        } else {
            TaskComment taskComment = comments.get(0);
            comment = taskComment.getText();
        }
        ExtendedInfoImpl.StringInfo directive = (ExtendedInfoImpl.StringInfo) ExtendedInfoImpl.createExtendedInfo(task.getDirective() != null && !task.getDirective().isEmpty() ? task.getDirective() : task.getType());
        ExtendedInfoImpl.DateInfo dueDate = (ExtendedInfoImpl.DateInfo) ExtendedInfoImpl.createExtendedInfo(task.getDueDate());
        WorkflowUtils.newEntry(document, user,
                taskName, WorkflowExtConstants.WORKFLOW_CATEGORY, comment, directive, dueDate);
    }

    /**
     * Get route model.
     *
     * @param document
     * @param session
     * @return
     */
    public static DocumentModel getRouteModelForDocument(DocumentModel document, CoreSession session) {
        DocumentRoutingService documentRoutingService = Framework.getLocalService(DocumentRoutingService.class);
        List<DocumentModel> routeModels = documentRoutingService.searchRouteModels(
                session, "");
        for (Iterator<DocumentModel> it = routeModels.iterator(); it.hasNext(); ) {
            DocumentModel route = it.next();
            Object graphRouteObj = route.getAdapter(GraphRoute.class);
            if (graphRouteObj instanceof GraphRoute) {
                String filter = ((GraphRoute) graphRouteObj).getAvailabilityFilter();
                if (!StringUtils.isBlank(filter)) {
                    if (checkActionFilter(document, session, filter)) {
                        return route;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check action filter.
     *
     * @param document
     * @param session
     * @param filterId
     * @return
     */
    private static boolean checkActionFilter(DocumentModel document, CoreSession session, String filterId) {
        ActionManager actionService = Framework.getService(ActionManager.class);
        return actionService.checkFilter(filterId, createActionContext(document, session));
    }

    /**
     * Create action context.
     *
     * @param document
     * @param session
     * @return
     */
    private static ActionContext createActionContext(DocumentModel document, CoreSession session) {
        ActionContext ctx;
        FacesContext faces = FacesContext.getCurrentInstance();
        if (faces == null) {
            ctx = new SeamActionContext();
        } else {
            ctx = new JSFActionContext(faces);
        }
        ctx.setCurrentDocument(document);
        ctx.setDocumentManager(session);
        ctx.setCurrentPrincipal((NuxeoPrincipal) session.getPrincipal());
        ctx.putLocalVariable("SeamContext", new SeamContextHelper());
        return ctx;
    }

    /**
     * Get document node from task.
     *
     * @param session
     * @param task
     * @return
     */
    public static DocumentModel getDocumentNodeFromTask(CoreSession session, Task task) {
        String nodeStepId = task.getVariable("document.routing.step");
        return session.getDocument(new IdRef(nodeStepId));
    }

    /**
     * Get target document given the task.
     *
     * @param task
     * @return
     */
    public static DocumentModel getTargetDocumentFromTask(CoreSession session, Task task) {
        return session.getDocument(new IdRef(task.getTargetDocumentId()));
    }

    /**
     * Clear comment property.
     *
     * @param node
     */
    public static void clearComments(CoreSession session, GraphNode node) {
        try {
            LOG.info("Clearing comments for " + node.getDocument().getId());
            node.getDocument().setPropertyValue("var-" + node.getId() + ":comment", "");
            session.saveDocument(node.getDocument());
        } catch (PropertyException e) {
            LOG.warn("Node " + node.getId() + " has not comment property");
        }
    }

    /**
     * Copy metadata from document source to destiny.
     *
     * @param source
     * @param destiny
     * @param schemas
     *            into the source document to propagate to destiny.
     * @return destiny modified
     */
    public static final Map<String, Serializable> copyMetadatas(DocumentModel source,
                                                    DocumentModel destiny, String... schemas) {

        if (source == null) {
            throw new IllegalArgumentException(
                    "Source document model must be not null");
        }
        if (destiny == null) {
            throw new IllegalArgumentException(
                    "Destiny document model must be not null");
        }
        Map<String, Serializable> copiedMetadata = new HashMap<>();
        for (String schema : schemas) {
            Map<String, Object> properties = source.getProperties(schema);
            if (properties == null) {
                continue;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Propagating schema " + schema + " ...");
            }
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String metadata = getMetadata(entry.getKey());
                Map<String, Object> destinyProperty = findProperty(destiny,
                        metadata);
                if (!destinyProperty.isEmpty()) {
                    String destinyPropertyValue = (String) destinyProperty
                            .get("key");
                    Serializable sourcePropertyValue = source
                            .getPropertyValue(entry.getKey());
                    destiny.setPropertyValue(destinyPropertyValue, sourcePropertyValue);
                    // Set copied metadata only with workflow variable name
                    copiedMetadata.put(getMetadata(destinyPropertyValue), sourcePropertyValue);
                } else {
                    LOG.debug("Document '" + destiny.getId()
                            + "' does not have the property " + metadata);
                }
            }
        }
        return copiedMetadata;
    }

    /**
     * Find a property into document model, in any schema that it has.
     *
     * @param doc
     * @param propertyName
     * @return property or null
     */
    private static Map<String, Object> findProperty(DocumentModel doc,
                                                    String propertyName) {
        Map<String, Object> property = new HashMap<String, Object>(2);
        String schemas[] = doc.getSchemas();
        for (String schema : schemas) {
            Map<String, Object> properties = doc.getProperties(schema);
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (getMetadata(entry.getKey()).equals(propertyName)) {
                    property.put("key", entry.getKey());
                    property.put("value", entry.getValue());
                }
            }
        }
        return property;
    }

    /**
     * Get metadata.
     *
     * @param property
     * @return
     */
    public static String getMetadata(String property) {
        if (property.contains(":")) {
            return property.split(":")[1];
        } else {
            return property;
        }
    }

    /**
     * Get task route document model.
     *
     * @param task
     * @return
     */
    public static DocumentModel getTaskRouteDocumentModel(Task task, CoreSession session) {
        final List<DocumentModel> doc = new ArrayList<>();
        final String routeDocId = task.getVariable(DocumentRoutingConstants.TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY);
        if (routeDocId != null) {
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() throws ClientException {
                    DocumentModel routeDoc = session.getDocument(new IdRef(routeDocId));
                    doc.add(routeDoc);
                }
            }.runUnrestricted();
        }
        if (!doc.isEmpty()) {
            return doc.get(0);
        }
        return null;
    }

    /**
     * Set ACL to task.
     *
     * @param task
     */
    public static void setACLToTask(CoreSession session, Task task) {
        if (task == null) {
            LOG.warn("Unable to set ACL with no task");
            return;
        }
        String readers = readConfigValue(session, "extendedWF:readers", String.class);
        if (readers != null) {
            String [] groups = readers.split(",");
            for (String group : groups) {
                if (group == null || group.isEmpty()) {
                    continue;
                }
                DocumentModel taskDocument = task.getDocument();
                ACP acp = taskDocument.getACP();
                ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
                acl.add(new ACE(group, SecurityConstants.READ, true));
                acp.addACL(acl);
                taskDocument.setACP(acp, true);
                session.saveDocument(taskDocument);
            }
        }
    }

    /**
     * Get task from route.
     *
     * @param task
     * @param route
     * @param session
     * @return
     */
    public static Task getTaskFromRoute(Task task, DocumentRoute route, CoreSession session) {
        TaskService taskService = Framework.getService(TaskService.class);
        List<Task> routeTasks = taskService.getAllTaskInstances(route.getDocument().getId(), session);
        for (Task routeTask : routeTasks) {
            LOG.info("Task of " + route.getModelName() + ": " + routeTask.getName());
            if (routeTask.getName().equals(task.getName())) {
                return routeTask;
            }
        }
        return null;
    }

    /**
     * Get a route version.
     *
     * @param route
     * @return
     */
    public static String getRouteVersion(DocumentRoute route) {
        if (route == null) {
            return "";
        }
        String title = route.getDocument().getTitle();
        Set<String> facets = route.getDocument().getFacets();
        for (String facet : facets) {
            if (facet.startsWith("facet-" + title)) {
                String tmpVersion = facet.replace("facet-" + title, "");
                if (tmpVersion.startsWith("_")) {
                    return tmpVersion.substring(1).replace("_", ".");
                } else if (tmpVersion.equals("")) {
                    return "0.0";
                } else {
                    return tmpVersion.replace("_", ".");
                }
            }
        }
        return null;
    }

    /**
     * Compare versions.
     * Method gotten from https://gist.github.com/antalindisguise
     *
     * @param v1
     * @param v2
     * @return
     */
    public static int compareVersion(String v1, String v2) {
        if (v1 == null || v2 == null) {
            return 0;
        }
        String[] vals1 = v1.split("\\.");
        String[] vals2 = v2.split("\\.");
        int i = 0;
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }
        else {
            return Integer.signum(vals1.length - vals2.length);
        }
    }

}
