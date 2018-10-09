package org.athento.nx.upgrade.api;

/**
 * User groups information class.
 */
public final class UsersInfo extends GenericInfo {

    Long totalUsers;

    public UsersInfo(String name) {
        super(name);
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

}
