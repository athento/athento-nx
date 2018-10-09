package org.athento.nuxeo.ui.restlet;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

import static org.jboss.seam.ScopeType.EVENT;

/**
 * Restlet helper.
 */
@Name("athentoRestHelper")
@Scope(EVENT)
public class AthentoRestHelper implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    /**
     * Get current file inline url.
     *
     * @return
     */
    public String getCurrentFileInlineUrl() {
        return getCurrentFileInlineUrl(navigationContext.getCurrentDocument(), "default", null);
    }

    /**
     * Get current file inline url.
     *
     * @param doc
     * @return
     */
    public String getCurrentFileInlineUrl(DocumentModel doc, String repo, HttpServletRequest req) {
        String baseURL = null;
        if (req == null) {
            baseURL = BaseURL.getBaseURL();
        } else {
            baseURL = BaseURL.getBaseURL(req);
        }
        if (baseURL.endsWith("/")) {
            baseURL = baseURL.substring(0, baseURL.length()-1);
        }
        return String.format("%s/restAPI/%s/%s/inlineFile", baseURL, repo, doc.getId());
    }


}
