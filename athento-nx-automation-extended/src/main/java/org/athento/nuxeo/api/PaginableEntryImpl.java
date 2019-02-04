package org.athento.nuxeo.api;

import org.nuxeo.ecm.automation.core.util.PaginablePageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;

import java.io.Serializable;
import java.util.Map;

public class PaginableEntryImpl extends PaginablePageProvider<Map<String, Serializable>> implements
        PaginableEntrySet {

    private static final long serialVersionUID = 1L;

    public PaginableEntryImpl(PageProvider<Map<String, Serializable>> provider) {
        super(provider);
    }
}
