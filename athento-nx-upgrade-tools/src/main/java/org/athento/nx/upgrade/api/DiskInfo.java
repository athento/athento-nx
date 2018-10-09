package org.athento.nx.upgrade.api;

/**
 * Disk information class.
 */
public final class DiskInfo extends GenericInfo {

    Long totalSpace;
    Long freeSpace;

    public DiskInfo(String name) {
        super(name);
    }

    public Long getTotalSpace() {
        return totalSpace;
    }

    public void setTotalSpace(Long totalSpace) {
        this.totalSpace = totalSpace;
    }

    public Long getFreeSpace() {
        return freeSpace;
    }

    public void setFreeSpace(Long freeSpace) {
        this.freeSpace = freeSpace;
    }
}
