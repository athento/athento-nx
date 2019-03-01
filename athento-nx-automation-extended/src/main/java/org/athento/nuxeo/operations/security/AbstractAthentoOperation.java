package org.athento.nuxeo.operations.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.operations.utils.AthentoOperationsHelper;
import org.nuxeo.ecm.automation.OperationContext;

import javax.servlet.http.HttpServletRequest;

/**
 * Abstract Athento operation.
 *
 * @since #AT-987
 */
public abstract class AbstractAthentoOperation {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(AbstractAthentoOperation.class);

    /** Void chain id. */
    public static final String VOIDCHAIN = "voidchain";

    /**
     * Check if IP has allowed access to operation execution.
     *
     * @param ctx context
     * @throws RestrictionException on error
     */
    protected void checkAllowedAccess(OperationContext ctx) throws RestrictionException {

        // Get remote ip
        HttpServletRequest request = (HttpServletRequest) ctx.get("request");
        if (request == null) {
            LOG.debug("Request info was null to manage controlled access.");
            return;
        }

        String remoteIp = request.getRemoteAddr();

        // Get extended config document
        String enabledIpsValue = AthentoOperationsHelper
                .readConfigValue(ctx.getCoreSession(), "automationExtendedConfig:enabledIPs");

        if (LOG.isTraceEnabled()) {
            LOG.trace("Enabled IPs " + enabledIpsValue);
        }

        // Check ip in list
        if (enabledIpsValue != null && !enabledIpsValue.isEmpty()) {
            if (!"*".equals(enabledIpsValue)) {
                String[] ips = enabledIpsValue.split(",");
                if (ips.length > 0) {
                    for (String ipp : ips) {
                        if (ipp.trim().equals(remoteIp)) {
                            return;
                        }
                    }
                    throw new RestrictionException(remoteIp + " has not allowed access.");
                }
            }
        }
    }

    /**
     * Check if operationId void chain.
     *
     * @param operationId
     * @return true if operationId is a void chain
     */
    protected boolean isVoidChain(String operationId) {
        return VOIDCHAIN.equalsIgnoreCase(operationId);
    }

}
