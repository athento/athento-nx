package org.athento.nuxeo.wf.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Schema utils.
 *
 */
public final class SchemaUtils {

    /**
     * System schemas.
     */
    private static String [] SYSTEM_SCHEMAS = { "dublincore", "common", "uid", "file", "files" };

    /**
     * Get valid schemas to propagate.
     *
     * @param schemas
     * @return
     */
    public static final List<String>getValidPropagatedSchemas(String...schemas) {
        List<String> valid = new ArrayList();
        List<String> systemSchemas = Arrays.asList(SYSTEM_SCHEMAS);
        for (String schema : schemas) {
            if (!systemSchemas.contains(schema)) {
                valid.add(schema);
            }
        }
        return valid;
    }

}
