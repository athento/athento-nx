package org.athento.nuxeo.ui.util;

import org.apache.commons.codec.binary.Base64;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Utils class.
 */
public final class Utils {

    private static final String TOKEN_ENDCHARS_CONTROL = "#control";

    /**
     * Check a simple valid token.
     *
     * @param token
     * @param document
     * @return
     */
    public static boolean validToken(String token, DocumentModel document) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String decodedToken = new String(Base64.decodeBase64(token));
        if (!decodedToken.endsWith(TOKEN_ENDCHARS_CONTROL)) {
            return false;
        }
        String changeToken = document.getChangeToken();
        if (changeToken == null) {
            return false;
        }
        String changeDecodedToken = decodedToken.replace(
                TOKEN_ENDCHARS_CONTROL, "");
        if (changeDecodedToken == null) {
            return false;
        }
        try {
            Long changeTokenTime = Long.valueOf(changeToken);
            Long changeDecodedTokenTime = Long.valueOf(changeDecodedToken);
            long time = changeTokenTime - changeDecodedTokenTime;
            return time < 2592000000L; // 30 days in millis
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
