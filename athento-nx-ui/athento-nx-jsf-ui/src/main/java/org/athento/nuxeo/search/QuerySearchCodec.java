package org.athento.nuxeo.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentPathCodec;

/**
 * Query Search codec.
 *
 * Based on SearchCodec.java in NX search to use query as parameter.
 */
public class QuerySearchCodec extends DocumentPathCodec {

    private static final Log log = LogFactory.getLog(QuerySearchCodec.class);

    public static final String PREFIX = "nxqsearch";

    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        return PREFIX;
    }

    @Override
    public boolean handleDocumentView(DocumentView docView) {
        return false;
    }

}
