package org.athento.nuxeo.ui.lifecycle;

/**
 * Lifecycle actions.
 */
public interface LifecycleAction {

    /**
     * Execute the current transition.
     *
     */
    void followTransition();

    /**
     * Check user in group.
     *
     * @param group
     * @return
     */
    boolean userInGroup(String group);

}
