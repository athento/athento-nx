package org.athento.nuxeo.ui.configuration;

/**
 * Created by victorsanchez on 19/05/16.
 */
public interface UserUIConfigurationAction {

    /**
     * Save content view configuration for user.
     *
     * @param contentView
     * @param columns
     * @param user
     */
    void saveContentViewConfiguration(String contentView, String [] columns, String user);

    /**
     * Load content view configuration.
     *
     * @param contentView
     * @param user
     */
    void loadContentViewConfiguration(String contentView, String user);


}
