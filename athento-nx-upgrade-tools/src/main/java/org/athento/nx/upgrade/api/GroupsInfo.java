package org.athento.nx.upgrade.api;

/**
 * Groups information class.
 */
public final class GroupsInfo extends GenericInfo {

    Long totalGroups;

    public GroupsInfo(String name) {
        super(name);
    }

    public Long getTotalGroups() {
        return totalGroups;
    }

    public void setTotalGroups(Long totalGroups) {
        this.totalGroups = totalGroups;
    }
}
