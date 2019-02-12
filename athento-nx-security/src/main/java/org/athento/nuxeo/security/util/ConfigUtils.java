package org.athento.nuxeo.security.util;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Config values.
 */
public final class ConfigUtils {

    public static final String CONFIG_PATH = "/ExtendedConfig";

    /**
     * Read config value.
     *
     * @param session is the core session
     * @param key is the key to get config value
     * @param defaultValue is the default value
     * @return config value
     */
    public static <T> T readConfigValue(CoreSession session, final String key, final T defaultValue) {
        final List<T> values = new ArrayList<>();
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                DocumentModel conf = session.getDocument(new PathRef(
                        ConfigUtils.CONFIG_PATH));
                Serializable value = conf.getPropertyValue(key);
                if (value != null) {
                    values.add((T) value);
                } else {
                    values.add(defaultValue);
                }
            }
        }.runUnrestricted();
        if (!values.isEmpty()) {
            return values.get(0);
        }
        return null;
    }

}
