package org.athento.nuxeo.operations;

import net.sf.json.JSONArray;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.operations.exception.AthentoException;
import org.athento.nuxeo.operations.security.AbstractAthentoOperation;
import org.athento.nuxeo.operations.utils.AthentoOperationsHelper;
import org.athento.utils.SecurityUtil;
import org.athento.utils.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.RecordSet;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

import java.util.HashMap;
import java.util.Map;


/**
 * @author athento
 */
@Operation(id = AthentoDocumentFindEntriesOperation.ID, category = "Athento", label = "Athento Document Find Entries", description = "Return a found entries")
public class AthentoDocumentFindEntriesOperation extends AbstractAthentoOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(AthentoDocumentFindEntriesOperation.class);

    private static final String QUERY_DIRECTORY_NAME = "saved_queries";

    public static final String ID = "Athento.DocumentFindEntries";

    public static final String DESC = "DESC";

    public static final String ASC = "ASC";

    @Context
    protected CoreSession session;

    /**
     * Operation context.
     */
    @Context
    protected OperationContext ctx;

    @Param(name = "query", required = false)
    protected String query;

    @Param(name = "queryId", required = false)
    protected String queryId;

    @Param(name = "maxResults", required = false)
    protected String maxResults = "20";

    @Param(name = "page", required = false)
    protected Integer page = 0;

    @Param(name = "pageSize", required = false)
    protected Integer pageSize = 20;

    @Param(name = "providerName", required = false)
    protected String providerName;

    @Param(name = "sortBy", required = false, description = "Sort by "
            + "properties (separated by comma)")
    protected String sortBy;

    @Param(name = "sortOrder", required = false, description = "Sort order, "
            + "ASC or DESC", widget = Constants.W_OPTION, values = {
            AthentoDocumentFindEntriesOperation.ASC,
            AthentoDocumentFindEntriesOperation.DESC})
    protected String sortOrder;

    @Param(name = "fieldList", required = false)
    protected StringList fieldList;

    @Param(name = "fieldComplex", required = false)
    protected StringList fieldComplex;

    @Param(name = "showCastingSource", required = false)
    protected boolean showCastingSource = false;

    /**
     * Return a stringify JSON into Blob.
     *
     * @return
     * @throws Exception
     */
    @OperationMethod
    public Blob run() throws Exception {
        // Check access
        checkAllowedAccess(ctx);
        // Get session from context
        session = ctx.getCoreSession();
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing query " + query);
            }
            if (queryId != null) {
                String tmpQuery = getQueryById(queryId);
                if (tmpQuery != null) {
                    query = tmpQuery;
                }
            } else {
                if (query != null) {
                    query = query.trim();
                }
                // Check if query is ciphered
                if (query.startsWith("{cipher}")) {
                    String secret = Framework.getProperty("athento.cipher.secret", null);
                    if (secret != null) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Query is ready to decrypt...");
                        }
                        query = query.substring(8);
                        query = SecurityUtil.decrypt(secret, query);
                    }
                }
            }
            Object input = null;
            String operationId = "Resultset.PageProvider";
            Map<String, Object> params = new HashMap<>();
            params.put("query", query);
            params.put("page", page);
            params.put("maxResults", maxResults);
            params.put("pageSize", pageSize);
            params.put("fieldList", fieldList);
            params.put("fieldComplex", fieldComplex);
            params.put("showCastingSource", showCastingSource);
            params.put("providerName", providerName);
            if (!StringUtils.isNullOrEmpty(sortBy)) {
                params.put("sortBy", sortBy);
                if (!StringUtils.isNullOrEmpty(sortOrder)) {
                    params.put("sortOrder", sortOrder);
                }
            }
            Object retValue = AthentoOperationsHelper.runOperation(operationId,
                    input, params, session);
            if (retValue instanceof RecordSet) {
                return Blobs.createBlob(JSONArray.fromObject(retValue).toString(), "application/json");
            } else {
                LOG.error("Unexpected return type for operation: "
                        + operationId);
                return null;
            }
        } catch (Exception e) {
            LOG.error(
                    "Unable to complete operation: "
                            + AthentoDocumentFindEntriesOperation.ID + " due to: "
                            + e.getMessage(), e);
            if (e instanceof AthentoException) {
                throw e;
            }
            AthentoException exc = new AthentoException(e.getMessage(), e);
            throw exc;
        }
    }

    /**
     * Get a query from a store-vocabulary given a query id.
     *
     * @param queryId is the query name
     * @return a query or null if it does not exist
     */
    private String getQueryById(String queryId) {
        DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
        Session dir = null;
        try {
            dir = directoryService.open(QUERY_DIRECTORY_NAME);
            DocumentModel entry = dir.getEntry(queryId);
            if (entry == null) {
                return null;
            }
            return (String) entry.getPropertyValue("vocabulary:label");
        } catch (DirectoryException e) {
            LOG.error("Unable to get query by id: " + queryId);
            return null;
        } finally {
            if (dir != null) {
                dir.close();
            }
        }
    }
}
