package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Rebuild a audit logs for a document.
 */
@Operation(id = RebuildLogsOperation.ID, category = "Athento", label = "Rebuild audit logs", description = "Rebuild audit logs for a document")
public class RebuildLogsOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(RebuildLogsOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.RebuildLogs";

    /** Context. */
    @Context
    protected OperationContext ctx;

    private AuditLogger auditLogger;

    @Param(name = "save", required = false, description = "Save permission after restructuration")
    private boolean save = false;

    /** Run. */
    @OperationMethod
    public DocumentModel run(DocumentModel doc) {

        if (LOG.isInfoEnabled()) {
            LOG.info("Rebuilding logs for " + doc.getId());
        }

        // Get versions from document
        List<DocumentModel> versions = ctx.getCoreSession().getVersions(doc.getRef());

        LOG.info("Total versions = " + versions.size());

        List<LogEntry> entries = new ArrayList<>();

        String currentLifeCycleState = null;

        for (DocumentModel version : versions) {
            // Create audit log when lifecycle changes
            String versionLifecycleState = version.getCurrentLifeCycleState();
            if (versionLifecycleState != null && !versionLifecycleState.equals(currentLifeCycleState)) {
                LOG.info("Add log for version " + version.getVersionLabel() + " with lifecycle " + currentLifeCycleState + " => " + versionLifecycleState);
                // Create audit log
                entries.add(createAuditLog(doc, version, versionLifecycleState));
                currentLifeCycleState = versionLifecycleState;
            }
        }

        AuditLogger logger = getAuditLogger();
        logger.addLogEntries(entries);

        return doc;

    }

    /**
     * Create an audit log.
     *
     * @param doc
     * @param lifecycleState
     */
    private LogEntry createAuditLog(DocumentModel doc, DocumentModel version, String lifecycleState) {
        LogEntry entry = new LogEntryImpl();
        entry.setCategory(DocumentEventCategories.EVENT_LIFE_CYCLE_CATEGORY);
        entry.setComment("Lifecycle change from version " + version.getVersionLabel());
        entry.setEventId("lifecycle_transition_event");
        entry.setEventDate(((GregorianCalendar) version.getPropertyValue("dc:modified")).getTime());
        entry.setDocLifeCycle(lifecycleState);
        entry.setDocUUID(doc.getId());
        entry.setPrincipalName(SecurityConstants.SYSTEM_USERNAME);
        entry.setLogDate(((GregorianCalendar) version.getPropertyValue("dc:modified")).getTime());
        return entry;
    }

    /**
     * Get audit logger.
     *
     * @return
     */
    private AuditLogger getAuditLogger() {
        if (auditLogger == null) {
            NXAuditEventsService auditService = (NXAuditEventsService) Framework.getRuntime().getComponent(
                    NXAuditEventsService.NAME);
            auditLogger = auditService.getBackend();
        }
        return auditLogger;
    }


}
