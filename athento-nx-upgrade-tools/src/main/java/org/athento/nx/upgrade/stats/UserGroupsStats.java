package org.athento.nx.upgrade.stats;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.api.GroupsInfo;
import org.athento.nx.upgrade.api.UsersInfo;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User and groups stats.
 */
public class UserGroupsStats implements StatCalculation {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(UserGroupsStats.class);

    public static final String STAT = "Users and Groups";

    /**
     * Run total calculations for user groups stats.
     *
     * @param session
     */
    @Override
    public void runCompleteCalculation(CoreSession session) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Run calculation stats for Users and groups...");
        }

        RegisterStatInfo statsInfo = RegisterStatInfo.getInstance();

        // Clear current stats
        statsInfo.clear(STAT);

        StatInfo userGroupsStats = new StatInfo(STAT, "Existing users and groups in instance");
        statsInfo.getStats().add(userGroupsStats);

        calculate(session, userGroupsStats, null);
    }

    /**
     * Calculate.
     *
     * @param session
     * @param statInfo
     * @param statName
     */
    public void calculate(CoreSession session, StatInfo statInfo, String statName) {
        RegisterStatInfo registerStatInfo = RegisterStatInfo.getInstance();
        if (statInfo == null) {
            statInfo = new StatInfo(STAT, "Existing users and groups in instance");
            registerStatInfo.getStats().add(statInfo);
        } else {
            statInfo = registerStatInfo.getStat(statInfo.getName());
            if (statInfo != null) {
                registerStatInfo.clearStat(statInfo.getName(), statName);
            }
        }
        Map<String, Long> totalUsersGroups = getTotalUsersAndGroups(session);

        if ("Users".equals(statName)) {
            Long totalUsers = totalUsersGroups.get("users");
            UsersInfo usersInfo = new UsersInfo("Users");
            usersInfo.setDescription("Numbers of users in Athento");
            usersInfo.setTotalUsers(totalUsers);
            statInfo.getEntries().add(0, usersInfo);
        } else if ("Groups".equals(statName)) {
            Long totalGroups = totalUsersGroups.get("groups");
            GroupsInfo groupsInfo = new GroupsInfo("Groups");
            groupsInfo.setDescription("Number of groups in Athento");
            groupsInfo.setTotalGroups(totalGroups);
            statInfo.getEntries().add(0, groupsInfo);
        } else {
            Long totalUsers = totalUsersGroups.get("users");
            UsersInfo usersInfo = new UsersInfo("Users");
            usersInfo.setDescription("Numbers of users in Athento");
            usersInfo.setTotalUsers(totalUsers);
            statInfo.getEntries().add(0, usersInfo);
            Long totalGroups = totalUsersGroups.get("groups");
            GroupsInfo groupsInfo = new GroupsInfo("Groups");
            groupsInfo.setDescription("Number of groups in Athento");
            groupsInfo.setTotalGroups(totalGroups);
            statInfo.getEntries().add(0, groupsInfo);
        }
    }

    /**
     * Get total for a users and groups.
     *
     * @param session
     * @return
     */
    protected Map<String, Long> getTotalUsersAndGroups(CoreSession session) {
        final Map<String, Long> totals = new HashMap<>();
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                Long totalUsers = 0L;
                Long totalGroups = 0L;
                UserManager userManager = Framework.getService(UserManager.class);
                List<String> groupIds = userManager.getGroupIds();
                for (String groupId : groupIds) {
                    totalUsers += userManager.getUsersInGroup(groupId).size();
                    totalGroups++;
                }
                totals.put("users", totalUsers);
                totals.put("groups", totalGroups);
            }
        }.runUnrestricted();
        return totals;
    }
}
