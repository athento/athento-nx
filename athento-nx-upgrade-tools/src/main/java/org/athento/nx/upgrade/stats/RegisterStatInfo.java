package org.athento.nx.upgrade.stats;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.api.GenericInfo;
import sun.net.www.content.text.Generic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Generic singleton class for stats register.
 */
public final class RegisterStatInfo implements Serializable {

    private static final Log LOG = LogFactory.getLog(RegisterStatInfo.class);

    /* Totals stats. */
    private List<StatInfo> stats = new ArrayList<>();

    private static RegisterStatInfo instance = null;

    private RegisterStatInfo() {}

    public synchronized List<StatInfo> getStats() {
        return stats;
    }

    public void setStats(List<StatInfo> stats) {
        this.stats = stats;
    }

    public StatInfo getStat(String stat) {
        for (StatInfo statInfo : stats) {
            if (statInfo.getName().equals(stat)) {
                return statInfo;
            }
        }
        return null;
    }

    public GenericInfo getStat(String name, String stat) {
        for (Iterator<StatInfo> it = stats.iterator(); it.hasNext();)    {
            StatInfo statInfo = it.next();
            if (statInfo.name.equals(name)) {
                for (Iterator<GenericInfo> entryIt = statInfo.getEntries().iterator(); entryIt.hasNext();) {
                    GenericInfo entry = entryIt.next();
                    if (entry.getName().equals(stat)) {
                        return entry;
                    }
                }
            }
        }
        return null;
    }

    public static RegisterStatInfo getInstance() {
        if (instance == null) {
            instance = new RegisterStatInfo();
        }
        return instance;
    }

    public synchronized void clear(String name) {
        for (Iterator<StatInfo> it = stats.iterator(); it.hasNext();)    {
            StatInfo statInfo = it.next();
            if (statInfo.name.equals(name)) {
                it.remove();
            }
        }
    }

    public synchronized void clearStat(String name, String stat) {
        for (Iterator<StatInfo> it = stats.iterator(); it.hasNext();)    {
            StatInfo statInfo = it.next();
            if (statInfo.name.equals(name)) {
                for (Iterator<GenericInfo> entryIt = statInfo.getEntries().iterator(); entryIt.hasNext();) {
                    GenericInfo entry = entryIt.next();
                    if (entry.getName().equals(stat)) {
                        entryIt.remove();
                        break;
                    }
                }
            }
        }
    }

    public synchronized void clearAll() {
        stats.clear();
    }
}

