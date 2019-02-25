package org.athento.nuxeo.search;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.jboss.seam.annotations.*;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.Map;

import static org.apache.commons.logging.LogFactory.getLog;
import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

/**
 * Seam to manage query search.
 */
@Name("querySearchUIActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class QuerySearchUIActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log LOG = getLog(QuerySearchUIActions.class);

    public static final String MAIN_TABS_SEARCH = "MAIN_TABS:qsearch";

    public static final String DEFAULT_CONTENT_VIEW = "query_search";

    public static final String SEARCH_VIEW_ID = "/search/qsearch.xhtml";

    public static final String SEARCH_CODEC = "querypathsearch";

    public static final String CONTENT_VIEW_NAME_PARAMETER = "contentViewName";

    public static final String DEFAULT_NXQL_QUERY = "U0VMRUNUICogRlJPTSBEb2N1bWVudCBXSEVSRSBlY206bWl4aW5UeXBlICE9ICdI" +
            "aWRkZW5Jbk5hdmlnYXRpb24nIEFORCBlY206aXNQcm94eSA9IDAgQU5EIGVjbTppc0NoZWNrZWRJblZlcnNpb24gPSAwIEFORCBlY206Y" +
            "3VycmVudExpZmVDeWNsZVN0YXRlICE9ICdkZWxldGVkJw==";

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected RestHelper restHelper;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected String currentPage;

    protected String pageSize;

    protected String query = DEFAULT_NXQL_QUERY;

    protected String contentViewName;

    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String loadPermanentLink(DocumentView docView) {
        restHelper.initContextFromRestRequest(docView);
        refreshAndRewind();
        return "qsearch";
    }

    /**
     * Refresh and rewind.
     */
    public void refreshAndRewind() {
        ContentView contentView = contentViewActions.getContentView(getContentViewName());
        if (contentView != null) {
            contentView.refreshAndRewindPageProvider();
        }
    }

    public String getSearchMainTab() {
        return MAIN_TABS_SEARCH;
    }

    public void setSearchMainTab(String tabs) {
        webActions.setCurrentTabIds(!StringUtils.isBlank(tabs) ? tabs : MAIN_TABS_SEARCH);
    }

    public String getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(String currentPage) {
        this.currentPage = currentPage;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getDecodedQuery() {
        String query = getQuery();
        if (Base64.isBase64(query)) {
            return new String(Base64.decodeBase64(getQuery()));
        } else {
            return query;
        }
    }

    public String getQuery() {
        if (query == null || query.isEmpty()) {
            query = DEFAULT_NXQL_QUERY;
        }
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    protected DocumentView computeDocumentView(DocumentModel doc) {
        return new DocumentViewImpl(new DocumentLocationImpl(documentManager.getRepositoryName(),
                doc != null ? new PathRef(doc.getPathAsString()) : null));
    }

    public String getContentViewName() {
        if (contentViewName == null || contentViewName.isEmpty()) {
            contentViewName = DEFAULT_CONTENT_VIEW;
        }
        return contentViewName;
    }

    public void setContentViewName(String contentViewName) {
        this.contentViewName = contentViewName;
    }

    public boolean isOnSearchView() {
        if (FacesContext.getCurrentInstance() == null) {
            return false;
        }
        UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
        if (viewRoot != null) {
            String viewId = viewRoot.getViewId();
            if (SEARCH_VIEW_ID.equals(viewId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get default destination folder from extended config.
     *
     * @return
     */
    public DocumentModel getDefaultDestinationFolder() {
        try {
            DocumentModel extendedConfig = documentManager.getDocument(new PathRef("/ExtendedConfig"));
            String defaultDestinationPath = (String) extendedConfig.getPropertyValue("automationExtendedConfig:defaultDestination");
            if (defaultDestinationPath != null && !defaultDestinationPath.isEmpty()) {
                return documentManager.getDocument(new PathRef(defaultDestinationPath));
            }
        } catch (DocumentNotFoundException e) {
            LOG.warn("Not found default destination folder in extended config: " + e.getMessage());
        }
        return null;
    }
}
