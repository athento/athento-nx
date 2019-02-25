package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.storage.sql.ra.ConnectionFactoryImpl;
import org.nuxeo.ecm.core.storage.sql.ra.ManagedConnectionFactoryImpl;

import javax.resource.ResourceException;

/**
 * Operations for ACL optimization.
 */
@Operation(id = ACLOptimizationOperation.ID, category = "Athento", label = "ACL optimization operations", description = "ACL optimization operations")
public class ACLOptimizationOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(ACLOptimizationOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.ACLOptimization";

    /** Available functions. */
    private static final String REBUILD_READ_ACLS = "REBUILD_READ_ACLS";
    private static final String UPDATE_READ_ACLS = "UPDATE_READ_ACLS";

    /** Session. */
    @Context
    protected CoreSession session;

    /** Function name. */
    @Param(name = "function", description = "Function to execute")
    protected String function;

    /** Run. */
    @OperationMethod
    public String run() throws Exception {
        org.nuxeo.ecm.core.storage.sql.Session sqlSession = getSession();
        if (REBUILD_READ_ACLS.equals(function)) {
            LOG.info("Rebuilding read acls...");
            sqlSession.rebuildReadAcls();
        } else if (UPDATE_READ_ACLS.equals(function)) {
            LOG.info("Updating read acls...");
            sqlSession.updateReadAcls();
        }
        return "OK";

    }

    /**
     * Get SQL session.
     *
     * @return sql session
     */
    public org.nuxeo.ecm.core.storage.sql.Session getSession() throws ResourceException {
        ManagedConnectionFactoryImpl managedConnectionFactory = new ManagedConnectionFactoryImpl(session.getRepositoryName());
        ConnectionFactoryImpl connectionFactory = (ConnectionFactoryImpl) managedConnectionFactory.createConnectionFactory();
        return connectionFactory.getConnection();
    }
}
