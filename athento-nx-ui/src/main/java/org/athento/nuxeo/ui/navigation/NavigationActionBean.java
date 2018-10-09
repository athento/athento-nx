package org.athento.nuxeo.ui.navigation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.ui.configuration.UserUIConfigurationAction;
import org.athento.nuxeo.ui.configuration.service.UIConfigurationManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.Observer;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.ui.web.util.SeamComponentCallHelper;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.security.Principal;
import java.util.*;

import static org.jboss.seam.ScopeType.SESSION;

/**
 * Navigation action.
 */
@Name("navigationAction")
@Scope(ScopeType.EVENT)
public class NavigationActionBean implements NavigationAction, Serializable {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(NavigationActionBean.class);

    @In
    protected ContentViewActions contentViewActions;

    private long currentPage = 0L;

    /**
     * Goto page.
     */
    @Override
    public void gotoPage(long page, String contentView) {
        this.currentPage = page - 1;
        PageProvider<?> provider = contentViewActions.getContentViewWithProvider(contentView)
                .getPageProvider();
        provider.setCurrentPageIndex(getCurrentPage());
        this.currentPage = provider.getCurrentPageIndex();
        if (this.currentPage > provider.getNumberOfPages() - 1) {
            this.currentPage = provider.getNumberOfPages() - 1;
        }
    }

    /**
     * Get current page.
     *
     * @return
     */
    public long getCurrentPage() {
        if (this.currentPage < 0) {
            this.currentPage = 0;
        }
        return this.currentPage;
    }

    /**
     * @param currentPage
     */
    @Override
    public void setCurrentPage(long currentPage) {
        this.currentPage = currentPage;
    }

    /**
     * Refresh page.
     */
    public void refreshPage(String contentView) {
        gotoPage(this.currentPage, contentView);
    }

    @Override
    public void previous(String contentView) {
        PageProvider<?> provider = contentViewActions.getContentViewWithProvider(contentView)
                .getPageProvider();
        provider.previousPage();
        this.currentPage = provider.getCurrentPageIndex();
    }

    @Override
    public void next(String contentView) {
        PageProvider<?> provider = contentViewActions.getContentViewWithProvider(contentView)
                .getPageProvider();
        provider.nextPage();
        this.currentPage = provider.getCurrentPageIndex();
    }

    @Override
    public void last(String contentView) {
        PageProvider<?> provider = contentViewActions.getContentViewWithProvider(contentView)
                .getPageProvider();
        provider.lastPage();
        this.currentPage = provider.getCurrentPageIndex();

    }

    @Override
    public void rewind(String contentView) {
        PageProvider<?> provider = contentViewActions.getContentViewWithProvider(contentView)
                .getPageProvider();
        provider.firstPage();
        this.currentPage = provider.getCurrentPageIndex();
    }
}
