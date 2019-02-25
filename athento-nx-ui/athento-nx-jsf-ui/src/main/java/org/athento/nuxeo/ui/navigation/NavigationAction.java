package org.athento.nuxeo.ui.navigation;

/**
 * Navigation actions wrap interface.
 */
public interface NavigationAction {

    /**
     * Goto page.
     */
    void gotoPage(long page, String contentView);

    /**
     * Refresh page.
     *
     * @param contentView
     */
    void refreshPage(String contentView);

    /**
     * Get current page.
     *
     * @return
     */
    long getCurrentPage();

    /**
     * Set current page.
     *
     * @param currentPage
     */
    void setCurrentPage(long currentPage);

    void previous(String contentView);

    void next(String contentView);

    void last(String contentView);

    void rewind(String contentView);

}
