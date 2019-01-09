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
            if (LOG.isInfoEnabled()) {
                LOG.info("Executing query " + query);
            }
            if (query != null) {
                query = query.trim();
            }
            String modifiedQuery = query;
            // Check if query is ciphered
            if (query.startsWith("{cipher}")) {
                String secret = Framework.getProperty("athento.cipher.secret", null);
                if (secret != null) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Query is ready to decrypt...");
                    }
                    query = query.substring(8);
                    modifiedQuery = SecurityUtil.decrypt(secret, query);
                }
            }
            Object input = null;
            String operationId = "Resultset.PageProvider";
            Map<String, Object> params = new HashMap<>();
            params.put("query", modifiedQuery);
            params.put("page", page);
            params.put("maxResults", maxResults);
            params.put("pageSize", pageSize);
            params.put("fieldList", fieldList);
            params.put("fieldComplex", fieldComplex);
            params.put("showCastingSource", showCastingSource);
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
}
