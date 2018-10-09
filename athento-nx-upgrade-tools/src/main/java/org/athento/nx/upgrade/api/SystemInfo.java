package org.athento.nx.upgrade.api;

import java.io.Serializable;

/**
 * System information class.
 */
public final class SystemInfo implements Serializable {

    String hostInfo;
    String uptime;

    public String getHostInfo() {
        return hostInfo;
    }

    public void setHostInfo(String hostInfo) {
        this.hostInfo = hostInfo;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }
}
