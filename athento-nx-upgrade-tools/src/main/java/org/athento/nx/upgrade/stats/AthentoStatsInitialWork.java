package org.athento.nx.upgrade.stats;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Athento stats worker.
 */
public class AthentoStatsInitialWork extends AbstractWork {

    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(AthentoStatsInitialWork.class);

    public static final String CATEGORY = "athentoStatistics";

    protected final String path;
    protected final String [] doctypes;

    /**
     * Constructor.
     *
     * @param repositoryName
     * @param path
     */
    public AthentoStatsInitialWork(String repositoryName, String [] doctypes, String path) {
        super(repositoryName + ":" + CATEGORY);
        setDocument(repositoryName, null);
        this.doctypes = doctypes;
        this.path = path;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getTitle() {
        return "Athento Statistics";
    }

    public void notifyProgress(float percent) {
        setProgress(new Progress(percent));
    }

    public void notifyProgress(long current, long total) {
        setProgress(new Progress(current, total));
    }

    @Override
    public void work() {
        final AthentoStatsInitialWork currentWorker = this;
        new UnrestrictedSessionRunner(repositoryName) {
            @Override
            public void run() {
                AthentoStatsService service = Framework.getService(AthentoStatsService.class);
                RegisterStatInfo.getInstance().clearAll();
                service.calculateDocumentTypeTotals(session, currentWorker.doctypes , path);
                service.calculateUsersAndGroupsTotals(session);
                service.calculateDiskTotals(session);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("End calculation with " + RegisterStatInfo.getInstance().getStats());
                }
            }
        }.runUnrestricted();
    }

}
