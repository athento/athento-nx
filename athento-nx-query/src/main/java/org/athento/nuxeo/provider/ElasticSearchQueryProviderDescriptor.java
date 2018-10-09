package org.athento.nuxeo.provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.query.ElasticSearchQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;

/**
 * Elastic search query provider.
 */
public class ElasticSearchQueryProviderDescriptor extends GenericPageProviderDescriptor {

    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(ElasticSearchQueryProviderDescriptor.class);

    public ElasticSearchQueryProviderDescriptor() {
        super();
        try {
            klass = (Class<PageProvider<?>>) Class.forName(ElasticSearchQueryAndFetchPageProvider.class.getName());
        } catch (ClassNotFoundException e) {
            LOG.error(e, e);
        }
    }

}