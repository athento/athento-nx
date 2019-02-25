package org.athento.nuxeo.ui.configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.ui.configuration.service.UIConfigurationManager;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.jboss.seam.ScopeType.SESSION;

/**
 * Created by victorsanchez on 19/05/16.
 */
@Name("userUIConfiguration")
@Scope(SESSION)
public class UserUIConfigurationBean implements UserUIConfigurationAction, Serializable {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(UserUIConfigurationBean.class);

    /**
     * Document manager.
     */
    @In(create = true, required = false)
    protected transient CloseableCoreSession documentManager;

    @In(create = true)
    protected transient ContentViewActions contentViewActions;

    @In(required = true)
    protected Principal currentUser;

    private boolean LOADED = false;

    /**
     * Update content view configuration.
     */
    @Observer("contentViewColumnsChanged")
    public void updateContentViewsConfiguration() {
        ContentViewService contentViewService = Framework.getService(ContentViewService.class);
        Set<String> contentViewNames = contentViewService.getContentViewNames();
        for (String contentViewName : contentViewNames) {
            ContentView contentView = contentViewActions.getContentView(contentViewName);
            try {
                if (contentView.getSearchDocumentModel() != null) {
                    List<String> columns = contentView.getCurrentResultLayoutColumns();
                    if (columns != null && !columns.isEmpty()) {
                        saveContentViewConfiguration(contentViewName,
                                contentView.getCurrentResultLayoutColumns().toArray(new String[0]),
                                this.currentUser.getName());
                    }
                }
            } catch (Exception e) {
                LOG.error("Content view " + contentViewName
                        + " had problems on change columns: " + e.getMessage());
            }
        }
    }

    /**
     * Save content view configuration.
     *
     * @param contentView
     * @param columns
     * @param user
     */
    public void saveContentViewConfiguration(String contentView, String[] columns, String user) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Saving content view configuration for " + user);
        }
        boolean needClose = false;
        if (this.documentManager == null) {
            this.documentManager = CoreInstance.openCoreSession("default");
            needClose = true;
        }
        // Save content view columns
        UIConfigurationManager uiConfigurationManager = Framework.getService(UIConfigurationManager.class);
        uiConfigurationManager.saveContentViewColumns(documentManager, contentView, columns, user);
        if (this.documentManager != null && needClose) {
            CoreInstance.closeCoreSession(this.documentManager);
        }
    }

    /**
     * Load configurations.
     */
    @Observer(EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED)
    public void loadConfiguration() {
        // Get all content-views
        ContentViewService contentViewService = Framework.getService(ContentViewService.class);
        Set<String> cvws = contentViewService.getContentViewNames();
        for (String cvw : cvws) {
            loadContentViewConfiguration(cvw, currentUser.getName());
        }
    }

    /**
     * Load content view configuration.
     *
     * @param contentView
     * @param user
     */
    public void loadContentViewConfiguration(String contentView, String user) {
        // Load content view columns
        UIConfigurationManager uiConfigurationManager = Framework.getService(UIConfigurationManager.class);
        String[] columns = uiConfigurationManager.getContentViewColumns(documentManager, contentView, user);
        if (columns != null && columns.length > 0) {
            this.contentViewActions.getContentView(contentView).setCurrentResultLayoutColumns(Arrays.asList(columns));
        }
    }


}
