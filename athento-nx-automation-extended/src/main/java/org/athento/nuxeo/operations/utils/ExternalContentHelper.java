package org.athento.nuxeo.operations.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.utils.FTPException;
import org.athento.utils.FTPUtils;
import org.nuxeo.ecm.automation.client.model.StreamBlob;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

import java.io.File;
import java.io.IOException;
import java.net.*;

/**
 * External content helper.
 * Use to manage get the external binaries to create or update documents.
 *
 * @author victorsanchez
 */
public final class ExternalContentHelper {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(ExternalContentHelper.class);

    /**
     * Add content from SFTP.
     *
     * @param path if the external content path
     * @return
     */
    public static Blob addContentFromSFTP(String path) {
        Blob blob = null;
        try {
            boolean remove = FTPUtils.checkRemoveRemoteFile(path);
            String remoteFilePath = FTPUtils.getRemoteFilePath(path);
            File remoteFile = FTPUtils.getFile(remoteFilePath, remove);
            if (remoteFile != null) {
                blob = new FileBlob(remoteFile);
                blob.setFilename(remoteFile.getName());
            }
        } catch (FTPException e) {
            LOG.error("Unable to set blob from external content SFTP", e);
        }
        return blob;
    }

    /**
     * Add content from URI.
     *
     * @param uri
     * @return
     */
    public static Blob addContentFromURI(String uri) {
        Blob blob = null;
        try {
            URI fileURI = new URI(uri);
            URL url = fileURI.toURL();
            URLConnection uc = url.openConnection();
            int contentLength = uc.getContentLength();
            if (contentLength == -1) {
                throw new IOException("File content is empty");
            }
            blob = new FileBlob(uc.getInputStream(), uc.getContentType(), uc.getContentEncoding());
            blob.setFilename(FilenameUtils.getName(url.getPath()));
        } catch (URISyntaxException | IOException e) {
            LOG.error("Unable to set blob from external content URI", e);
        }
        return blob;
    }
}
