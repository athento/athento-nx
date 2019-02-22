package org.athento.nx.upgrade.stats;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.api.DiskInfo;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Disk stats.
 */
public class DiskStats implements StatCalculation {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(DiskStats.class);

    public static final String STAT = "Disk";

    /**
     * Run total calculations for user groups stats.
     *
     * @param session
     */
    @Override
    public void runCompleteCalculation(CoreSession session) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Run calculation stats for Disks...");
        }

        RegisterStatInfo statsInfo = RegisterStatInfo.getInstance();

        StatInfo diskStats = new StatInfo(STAT, "Disk space quotas");
        statsInfo.getStats().add(diskStats);

        calculate(diskStats);

    }

    public void calculate(StatInfo statInfo) {
        RegisterStatInfo registerStatInfo = RegisterStatInfo.getInstance();
        if (statInfo == null) {
            statInfo = new StatInfo(STAT, "Disk space quotas");
            registerStatInfo.getStats().add(statInfo);
        } else {
            statInfo = registerStatInfo.getStat(statInfo.getName());
            if (statInfo != null) {
                registerStatInfo.clearStat(STAT, statInfo.getName());
            }
        }
        String repoPath = Framework.getProperty("nuxeo.data.dir");
        try {
            FileStore store = Files.getFileStore(Paths.get(repoPath));
            DiskInfo diskInfo = new DiskInfo(STAT);
            diskInfo.setDescription("Information about repository disk available and total space");
            diskInfo.setTotalSpace(store.getTotalSpace());
            diskInfo.setFreeSpace(store.getUnallocatedSpace());
            statInfo.getEntries().add(0, diskInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
