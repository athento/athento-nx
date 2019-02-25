package org.athento.nuxeo.ui.configuration.service;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Created by victorsanchez on 19/05/16.
 */
public interface UIConfigurationManager {

    /**
     * Save content view columns of an user.
     *
     * @param session
     * @param contentView
     * @param columns
     * @param user
     */
    void saveContentViewColumns(CoreSession session, String contentView, String [] columns, String user);

    /**
     * Get content view columns for an user.
     *
     * @param session
     * @param contentView
     * @param user
     * @return
     */
    String [] getContentViewColumns(CoreSession session, String contentView, String user);
}
