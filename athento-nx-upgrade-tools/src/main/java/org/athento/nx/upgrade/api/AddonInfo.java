package org.athento.nx.upgrade.api;

import java.io.Serializable;

/**
 * Addon information.
 */
public final class AddonInfo implements Serializable {

    String name;
    String version;
    String status;
    boolean installed;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }
}
