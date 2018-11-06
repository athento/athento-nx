package org.athento.nuxeo.wf.api;

import java.util.Arrays;

/**
 * Routing constants.
 */
public final class RoutingConstants {

    public static String [] ROUTING_CORE_METADATA = { "collapsible", "order", "priority", "lfTransitions", "metadataAssignment" };

    public static String TASK_IGNORE_ASSIGNMENT_NOTIFICATION = "task.ignore.assignment.notification";

    public static String UPGRADE_EVENT_NAME = "upgradeWorkflowEvent";

    /**
     * Check if a property is a core property.
     *
     * @param property
     * @return
     */
    public static boolean isCoreRoutingProperty(String property) {
        return Arrays.asList(ROUTING_CORE_METADATA).contains(property);
    }
}
