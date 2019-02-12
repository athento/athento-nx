package org.athento.nuxeo.security.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.Tika;
import org.athento.nuxeo.security.api.MimetypeException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mimetype utils.
 *
 * @author pacoalias
 */
public final class MimeUtils {

    /** Log. */
    private static Log LOG = LogFactory.getLog(MimeUtils.class);

    private static List<String> includedDocumentTypes = new ArrayList<>();
    private static List<String> mimeTypesAllowed = new ArrayList<>();

    private static final String DEFAULT_DOCUMENT_TYPES_RESTRICTED = "File";
    private static final String DEFAULT_MIMETYPES_ALLOWED = "application/vnd.oasis.opendocument.text," +
                                                            "text/xml, text/html, text/plain, text/rtf, text/csv, text/css," +
                                                            "application/msword, application/msexcel, application/vnd.ms-excel, application/vnd.ms-powerpoint," +
                                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document," +
                                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet," +
                                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.template," +
                                                            "application/vnd.sun.xml.writer, application/vnd.sun.xml.writer.template, " +
                                                            "application/vnd.oasis.opendocument.text," +
                                                            "application/vnd.oasis.opendocument.text-template," +
                                                            "audio/ogg, video/ogg, application/ogg, audio/wav" +
                                                            "application/wordperfect, application/rtf, application/vnd.ms-outlook," +
                                                            "video/mpeg, video/quicktime, application/visio, audio/midi," +
                                                            "audio/mp3, image/gif, image/png, image/jpg, image/jpeg, image/tiff,"+
                                                            "application/pdf, application/x-gzip, application/csv, audio/aac, video/x-msvideo";

    private static final String PROPERTY_DOCUMENT_TYPES = "plugin.athento-nx-security-limit-file-upload-mime-types.documentTypesChecked";
    private static final String PROPERTY_MIMETYPES_ALLOWED = "plugin.athento-nx-security-limit-file-upload-mime-types.mimeTypesAllowed";
    private static final String XPATH_FILE_CONTENT = "file:content";

    static {
        includedDocumentTypes.addAll(Arrays.stream(DEFAULT_DOCUMENT_TYPES_RESTRICTED.split(",")).map(String::trim).collect(Collectors.toList()));
        mimeTypesAllowed.addAll(Arrays.stream(DEFAULT_MIMETYPES_ALLOWED.split(",")).map(String::trim).collect(Collectors.toList()));
        String mimeTypesAllowedValue = Framework
                .getProperty(PROPERTY_MIMETYPES_ALLOWED, DEFAULT_MIMETYPES_ALLOWED);
        if (LOG.isInfoEnabled()) {
            LOG.info("Framework Property ["
                    + PROPERTY_MIMETYPES_ALLOWED + "] value ["
                    + mimeTypesAllowedValue + "]");
        }
        if (mimeTypesAllowedValue != null) {
            mimeTypesAllowed.addAll(Arrays.stream(mimeTypesAllowedValue.split(",")).map(String::trim).collect(Collectors.toList()));
        } else {
            LOG.warn("No mimeTypes are restricted!!. To restrict mimetypes uploadable set property "
                    + PROPERTY_MIMETYPES_ALLOWED);
        }
        String documentTypesTraced = Framework.getProperty(
                PROPERTY_DOCUMENT_TYPES);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Framework property "
                    + PROPERTY_DOCUMENT_TYPES
                    + " value: " + documentTypesTraced + ")");
        }
        if (documentTypesTraced != null) {
            if (!"all".equalsIgnoreCase(documentTypesTraced)) {
                includedDocumentTypes.addAll(Arrays.stream(documentTypesTraced.split(",")).map(String::trim).collect(Collectors.toList()));
            } else {
                includedDocumentTypes.clear();
            }
        }
        if (includedDocumentTypes == null) {
            LOG.warn("No document types are traced!!. Set property "
                    + PROPERTY_DOCUMENT_TYPES);
        }
    }

    /**
     * Get included document types.
     *
     * @param doc
     * @return
     */
    public static List<String> getIncludedDocumentTypes(DocumentModel doc) {
        // Getting allowed document types
        String allowedDocumentTypes = ConfigUtils.readConfigValue(doc.getCoreSession(), "mimetypes_extendedconfig:allowedDoctypes", "");
        if (!"".equals(allowedDocumentTypes)) {
            includedDocumentTypes = Arrays.stream(allowedDocumentTypes.split(",")).map(String::trim).collect(Collectors.toList());
        }
        return includedDocumentTypes;
    }

    /**
     * Get mimetypes allowed.
     *
     * @param doc
     * @return
     */
    public static List<String> getMimeTypesAllowed(DocumentModel doc) {
        // Getting allowed mimetypes
        String allowedMimetypes = ConfigUtils.readConfigValue(doc.getCoreSession(), "mimetypes_extendedconfig:allowedMimetypes", "");
        if (!"".equals(allowedMimetypes)) {
            mimeTypesAllowed = Arrays.stream(allowedMimetypes.split(",")).map(String::trim).collect(Collectors.toList());
        }
        return mimeTypesAllowed;
    }

    /**
     * Check mimetypes.
     *
     * @param doc
     * @throws MimetypeException
     */
    public static void checkMimeType(DocumentModel doc) throws MimetypeException {
        if (!doc.hasSchema("file")) {
            return;
        }
        List<String> allowedDocumentTypes = getIncludedDocumentTypes(doc);
        if (allowedDocumentTypes == null || allowedDocumentTypes.isEmpty()) {
            LOG.debug("Included document types is undefined, no mimetypes checking executed.");
            return;
        }
        if (getMimeTypesAllowed(doc) == null) {
            LOG.debug("Included mimetypes, no checking executed.");
            return;
        }
        String documentType = doc.getDocumentType().getName();
        if (LOG.isDebugEnabled()) {
            LOG.info("Checking mimetype for doctype: " + documentType);
        }
        if (isWatchedDocumentType(documentType)) {
            try {
                checkMimeType(doc, (Blob) doc
                        .getPropertyValue(XPATH_FILE_CONTENT));
            }
            catch (MimetypeException e) {
                LOG.warn("Removing mimetype in main content");
                // Remove content or files
                doc.setPropertyValue(XPATH_FILE_CONTENT, null);
                throw e;
            }
            if (doc.hasSchema("files")) {
                List<Map<String, Serializable>> validFiles = new ArrayList<>();
                List<String> invalidFiles = new ArrayList<>();
                List<Map<String, Serializable>> files = (List<Map<String, Serializable>>) doc.getPropertyValue("files:files");
                for (Map<String, Serializable> file : files) {
                    try {
                        checkMimeType(doc, (Blob) file.get("file"));
                        validFiles.add(file);
                    } catch (MimetypeException e) {
                        LOG.warn("Removing mimetype in files for " + file.get("filename"));
                        invalidFiles.add((String) file.get("filename"));
                    }
                }
                doc.setPropertyValue("files:files", (Serializable) validFiles);
                if (!invalidFiles.isEmpty()) {
                    throw new MimetypeException("Attachments invalids: " + invalidFiles);
                }
            }
        }
    }

    /**
     * Check mimetype for a blob.
     *
     * @param doc is the document to check
     * @param blob is the blob
     * @throws MimetypeException when mimetype is not allowed
     */
    public static void checkMimeType(DocumentModel doc, Blob blob) throws MimetypeException {
        if (blob == null) {
            return;
        }
        boolean allowed;
        String mimeType = blob.getMimeType();
        // Check if mimetype is empty
        if (isEmpty(mimeType)) {
            boolean emptyEnabled = ConfigUtils.readConfigValue(doc.getCoreSession(), "mimetypes_extendedconfig:allowEmptyMimetype", false);
            if (emptyEnabled) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Mimetype empty is enabled, blob is allowed.");
                }
                return;
            }
        }
        boolean extractAndCompare = ConfigUtils.readConfigValue(doc.getCoreSession(), "mimetypes_extendedconfig:useExtract", false);
        if (extractAndCompare) {
            // Extracting mimetype from blob to check it
            String extractedMimetype = extractMimetype(blob.getFile());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Mimetype extracted " + extractedMimetype);
            }
            if (extractedMimetype != null && !extractedMimetype.equals(mimeType)) {
                throw new MimetypeException("Mimetype " + mimeType + " and extracted " + extractedMimetype + " are different.");
            }
        }
        allowed = isMimeTypeAllowed(mimeType);
        if (!allowed) {
            throw new MimetypeException("This mimeType is NOT allowed: " + mimeType);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("This mimetype " + mimeType + " is allowed");
            }
        }
    }

    /**
     * Check if mimetype is empty (or null).
     *
     * @param mimeType to check
     * @return
     */
    private static boolean isEmpty(String mimeType) {
        return mimeType == null || "".equals(mimeType);
    }

    /**
     * Extracting mimetype from file. Using Apache(c) Tika.
     *
     * @param file
     * @return
     */
    private static String extractMimetype(File file) {
        try {
            Tika tika = new Tika();
            return tika.detect(file);
        } catch (IOException e) {
            LOG.warn("Unable to extract mimetype for file document", e);
        }
        return null;
    }

    /**
     * Is watched documenttype.
     *
     * @param documentType
     * @return
     */
    private static boolean isWatchedDocumentType (String documentType) {
        if (includedDocumentTypes.isEmpty()) {
            // all documents are allowed
            return true;
        }
        for (String dt: includedDocumentTypes) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("  documentType watched [" + dt + "]");
            }
            if (dt.equalsIgnoreCase(documentType)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(" WATCHED documentType [" + documentType + "]");
                }
                return true;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(" not watched documentType [" + documentType + "]");
        }

        return false;
    }

    /**
     * Check if mimetype is allowed.
     *
     * @param mimeType
     * @return
     */
    private static boolean isMimeTypeAllowed (String mimeType) {
        for (String mt : mimeTypesAllowed) {
            if (mt.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }
        return false;
    }

}
