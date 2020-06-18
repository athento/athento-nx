package org.athento.nx.upgrade.worker;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.blob.binary.AESBinaryManager;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Export orphan worker.
 *
 * @author victorsanchez
 */
public class ExportOrphanWorker extends AbstractWork {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(ExportOrphanWorker.class);

    /**
     * Title.
     */
    public static final String TITLE = "Exporting Orphan documents";

    /**
     * Category.
     */
    public static final String CATEGORY = "exportOrphan";

    /** Start and end dates. */
    protected Date startDate;
    protected Date endDate;

    /** Total documents exported. */
    private long totalDocs = 0;

    /** Download information. */
    private String downloadPath = null;


    /** Repo path. */
    private String repoPath;


    /**
     * Constructor.
     *
     * @param startDate
     * @param endDate
     * @param downloadPath
     * @param repoPath
     */
    public ExportOrphanWorker(DocumentModel doc, Date startDate, Date endDate, String downloadPath, String repoPath) {
        this.session = doc.getCoreSession();
        this.startDate = startDate;
        this.endDate = endDate;
        this.downloadPath = downloadPath;
        this.repoPath = repoPath;
    }

    /**
     * Get title of worker.
     *
     * @return title
     */
    @Override
    public String getTitle() {
        return getCategory();
    }

    /**
     * Get category.
     *
     * @return category
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /**
     * Work handler.
     *
     * @throws Exception on error
     */
    @Override
    public void work() {
        openSystemSession();
        try {

            if (LOG.isInfoEnabled()) {
                LOG.info("Starting exporting orphan documents worker for " + startDate + " to " + endDate + ")");
            }

            // Find documents between two dates
            List<File> files = findFilesIntoRepo();

            // Write documents
            writeFiles(files);

            if (LOG.isInfoEnabled()) {
                LOG.info("End orphan export to " + downloadPath);
            }
        } catch (Exception e) {
            LOG.error("Unable to export to Orphan Exporting", e);
        }
    }

    /**
     * Find files into repository between two dates.
     *
     * @return
     */
    private List<File> findFilesIntoRepo() throws IOException {
        File repoDir = new File(this.repoPath);
        if (!repoDir.exists()) {
            throw new IOException("Repository directory is not found");
        }
        if (!repoDir.isDirectory()) {
            throw new IOException("Repository file must be a directory");
        }
        return getFilesBetweenDates(repoDir.listFiles());
    }

    /**
     * Find files between dates.
     *
     * @param fileList
     * @return
     */
    private List<File> getFilesBetweenDates(File [] fileList) {
        if (fileList.length == 0) {
            return Collections.emptyList();
        }
        List<File> files = new ArrayList<>();
        for (File f : fileList) {
            if (f.isDirectory()) {
                LOG.info("Checking dir " + f.getName());
                files.addAll(getFilesBetweenDates(f.listFiles()));
            } else {
                LOG.info("Checking file " + f.getName());
                if (startDate.before(new Date(f.lastModified())) && endDate.after(new Date(f.lastModified()))) {
                    // Checking if digest is orphan
                    DocumentModelList docs = this.session.query("SELECT * FROM Document WHERE file:content/digest = '" + f.getName() + "'");
                    if (docs.size() == 0) {
                        LOG.info("Is orphan doc " + f.getName());
                        files.add(f);
                    }
                }
            }
        }
        return files;
    }


    /**
     * Write documents.
     *
     * @param files
     * @exception IOException on write error
     */
    private void writeFiles(List<File> files) {
        TransactionHelper.commitOrRollbackTransaction();
        try {
            AESBinaryManager binaryManager = new AESBinaryManager();
            TransactionHelper.startTransaction();
            File downloadDir = new File(downloadPath);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            for (File file : files) {
                String digest = file.getName();
                Binary binary = binaryManager.getBinary(digest);
                File decodedFile = binary.getFile();
                File f = new File(downloadPath + "/" + digest);
                LOG.info("Copy binary " + f.getAbsolutePath() + "...");
                try (OutputStream os = new FileOutputStream(f)) {
                    if (decodedFile.exists()) {
                        IOUtils.copy(binary.getStream(), os);
                    } else {
                        LOG.warn("Unable to download "
                                + digest + " because it doesn't exists");
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error writing documents", e);
            TransactionHelper.commitOrRollbackTransaction();
        }
    }


}
