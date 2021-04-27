package org.athento.nx.upgrade.worker;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Transaction;
import org.nuxeo.ecm.automation.client.model.StreamBlob;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.restlet.util.DateUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Export massive worker.
 *
 * @author victorsanchez
 */
public class ExportMassiveCSVWorker extends AbstractWork {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(ExportMassiveCSVWorker.class);

    private static final char DEFAULT_DELIMITER = ';';
    private static final char DEFAULT_QOUTE = '"';

    /**
     * Title.
     */
    public static final String TITLE = "Exporting CSV Masive";

    /**
     * Category.
     */
    public static final String CATEGORY = "exportCSV";

    /**
     * Document root.
     */
    protected DocumentModel root;

    /** Doctype. */
    protected String doctype;

    /** Doctypes. */
    protected String [] doctypes;

    /** File destiny path. */
    protected String destinyPathFile;

    /** Metadata to export. */
    protected List<String> metadatas;

    /** Delimiter. */
    private char delimiter = DEFAULT_DELIMITER;

    /** Quote. */
    private char quote = DEFAULT_QOUTE;

    /** Total documents exported. */
    private long totalDocs = 0;

    /** Download information. */
    private String downloadPath = null;


    /**
     * Constructor.
     *
     * @param doc
     * @param doctype
     * @param metadatas
     * @param destinyPathFile
     */
    public ExportMassiveCSVWorker(DocumentModel doc, String doctype,
                                  List<String> metadatas, String destinyPathFile) {
        this.session = doc.getCoreSession();
        this.root = doc;
        if (doctype.contains(",")) {
            this.doctypes = doctype.split(",");
        } else {
            this.doctype = doctype;
        }
        this.destinyPathFile = destinyPathFile;
        this.metadatas= metadatas;
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
     * Set exported metadatas.
     *
     * @param metadatas
     */
    public void setExportedMetadatas(List<String> metadatas) {
        this.metadatas= metadatas;
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
                LOG.info("Starting exporting CSV massive worker with " + root + ")");
            }

            // Open CSV file and get CSV printer
            CSVPrinter printer = openCSVFile();

            // Write documents
            writeDocuments(printer, root.getRef());

            // Close printer
            printer.close();

            if (LOG.isInfoEnabled()) {
                LOG.info("End massive export CSV to " + destinyPathFile);
            }
        } catch (Exception e) {
            LOG.error("Unable to export to CSV Massive", e);
        }
    }

    /**
     * Write documents.
     *
     * @param printer
     * @param ref
     * @exception IOException on write error
     */
    private void writeDocuments(CSVPrinter printer, DocumentRef ref) {
        TransactionHelper.commitOrRollbackTransaction();
        try {
            TransactionHelper.startTransaction();
            DocumentModelList docList = session.getChildren(ref);
            if (LOG.isInfoEnabled()) {
                LOG.info("Saving children of " + ref + ": " + docList.size() + ". Total: " + totalDocs);
            }
            for (DocumentModel doc : docList) {
                if (this.doctype != null) {
                    if (this.doctype.equals(doc.getType())) {
                        printer.printRecord(extractCSVLine(doc));
                        totalDocs++;
                    }
                } else {
                    if (this.doctypes != null) {
                        if (exportDoctype(doc.getType())) {
                            printer.printRecord(extractCSVLine(doc));
                            totalDocs++;
                        }
                    }
                }
                if (doc.isFolder()) {
                    writeDocuments(printer, doc.getRef());
                }
            }
        } catch (Exception e) {
            LOG.error("Error writing documents", e);
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    /**
     * Check if a doctype will be exported or not.
     *
     * @param doctype
     * @return
     */
    private boolean exportDoctype(String doctype) {
        if (this.doctypes != null) {
            for (String dtype : this.doctypes) {
                if (dtype.trim().equals(doctype)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Extract CSV line from document.
     *
     * @param doc
     * @return array of string values
     */
    private List<String> extractCSVLine(DocumentModel doc) {
        List<String> fields = new ArrayList<>();
        for (String field : this.metadatas) {
            if (field.startsWith("ecm:")) {
                if ("ecm:uuid".equals(field)) {
                    fields.add(doc.getId());
                } else if ("ecm:name".equals(field)) {
                    fields.add(doc.getName());
                } else if ("ecm:path".equals(field)) {
                    fields.add(doc.getPathAsString());
                } else if ("ecm:currentLifeCycleState".equals(field)) {
                    fields.add(doc.getCurrentLifeCycleState());
                } else if ("ecm:primaryType".equals(field)) {
                        fields.add(doc.getType());
                } else if ("ecm:source".equals(field)) {
                    fields.add(doc.getSourceId());
                } else if ("ecm:tag".equals(field)) {
                    fields.add(getTags(doc));
                } else {
                    fields.add("");
                }
            } else {
                String metadata = field;
                String cast = null;
                if (field.contains("::")) {
                    metadata = field.split("::")[0];
                    cast = field.split("::")[1];
                }
                String propertyValue = "";
                String prefix = metadata.split(":")[0];
                if (hasSchema(doc, prefix)) {
                    try {
                        Property prop = doc.getProperty(metadata);
                        if (prop.isScalar()) {
                            // FIX
                            if (metadata.equals("file:filename")) {
                                Blob blob = (Blob) doc.getPropertyValue("file:content");
                                if (blob != null) {
                                    propertyValue = blob.getFilename();
                                }
                            } else {
                                Serializable value = prop.getValue();
                                if (cast != null) {
                                    try {
                                        if ("vocabulary".equals(cast)) {
                                            String vocabulary = field.split("::")[2];
                                            if (!"".equals(vocabulary)) {
                                                propertyValue = getVocabularyEntry(vocabulary, (String) value);
                                            }
                                        }
                                    } catch (IndexOutOfBoundsException ioe) {
                                        LOG.info("Error casting column", ioe);
                                    }
                                } else {
                                    if (value != null) {
                                        if (value instanceof GregorianCalendar) {
                                            propertyValue = DateUtils.format(((GregorianCalendar) value).getTime(), "dd-MM-yyyy HH:mm:ss");
                                        } else {
                                            propertyValue = String.valueOf(value);
                                        }
                                    }
                                }
                            }
                        } else if (prop.isComplex()) {
                            if (prop instanceof BlobProperty) {
                                try {
                                    LOG.info("Document " + doc.getId() + " has content");
                                    Blob blob = (Blob) doc.getPropertyValue(metadata);
                                    if (blob != null) {
                                        String digest = blob.getDigest();
                                        propertyValue = String.join("/", digest.substring(0, 2), digest.substring(2, 4), digest);
                                        // Manage download
                                        if (downloadPath != null) {
                                            String subdirs = digest.substring(0, 2) + "/" + digest.substring(2, 4);
                                            File dirs = new File(downloadPath + "/" + subdirs);
                                            dirs.mkdirs();
                                            File f = new File(downloadPath + "/" + subdirs + "/" + blob.getDigest());
                                            LOG.info("Copy binary " + f.getAbsolutePath() + "...");
                                            try (OutputStream os = new FileOutputStream(f)) {
                                                if (blob.getFile().exists()) {
                                                    IOUtils.copy(blob.getStream(), os);
                                                } else {
                                                    LOG.warn("Unable to download "
                                                            + blob.getDigest() + " because it doesn't exists");
                                                }
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    LOG.error("Unable to download file, please check binary in repo directory.", e);
                                }
                            }
                        }
                    } catch (PropertyException e) {
                        LOG.error("Property " + metadata + " is not found, please check. " +
                                "Value will be empty for this property.");
                    }
                } else {
                    propertyValue = "";
                }
                fields.add(propertyValue);
            }
        }

        return fields;
    }

    /**
     * Get tags of document.
     *
     * @param doc
     * @return
     */
    private String getTags(DocumentModel doc) {
        TagService tagService = getTagService();
        if (tagService == null) {
            return "";
        }
        List<Tag> tags = tagService.getDocumentTags(this.session, doc.getId(), null);
        return tags.stream().map(t -> t.getLabel()).collect(Collectors.joining(","));
    }

    /**
     * Get tag service.
     *
     * @return
     */
    protected TagService getTagService() {
        TagService tagService = Framework.getService(TagService.class);
        return tagService.isEnabled() ? tagService : null;
    }

    /**
     * Check if document has the schema by prefix.
     *
     * @param doc
     * @param prefix
     * @return
     */
    private boolean hasSchema(DocumentModel doc, String prefix) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Schema schema = schemaManager.getSchema(prefix);
        if (schema == null) {
            schema = schemaManager.getSchemaFromPrefix(prefix);
        }
        return schema != null && doc.hasSchema(schema.getName());
    }


    /**
     * Get vocabulary entry.
     *
     * @param vocabularyName
     * @param entryId
     * @return
     */
    public static String getVocabularyEntry(String vocabularyName, String entryId) {
        Session directorySession = null;
        try {
            directorySession = getDirectoryService().open(vocabularyName);
            DocumentModel entry = directorySession.getEntry(entryId);
            if (entry == null) {
                return "";
            }
            if (entry.hasSchema("xvocabulary")) {
                return (String) entry.getPropertyValue("xvocabulary:label");
            } else {
                return (String) entry.getPropertyValue("vocabulary:label");
            }
        } finally {
            if (directorySession != null) {
                directorySession.close();
            }
        }
    }


    /**
     * Get directory service.
     *
     * @return
     */
    private static DirectoryService getDirectoryService() {
        return Framework.getService(DirectoryService.class);
    }


    /**
     * Open CSV File.
     *
     * @return CSVPrinter
     * @throws IOException
     */
    private CSVPrinter openCSVFile() throws IOException {
        if (destinyPathFile == null) {
            throw new IOException("Destiny file must be not null");
        }
        FileWriter out = new FileWriter(this.destinyPathFile);
        return new CSVPrinter(out, CSVFormat.DEFAULT
                .withHeader(getHeaders())
                .withDelimiter(this.delimiter)
                .withQuoteMode(QuoteMode.ALL)
                .withQuote(this.quote));
    }

    /**
     * Get headers.
     *
     * @return
     */
    private String [] getHeaders() {
        List<String> headers = new ArrayList<>();
        for (String metadata : metadatas) {
            if (metadata.contains("::")) {
                headers.add(metadata.split("::")[0]);
            } else {
                headers.add(metadata);
            }
        }
        return headers.toArray(new String[0]);
    }

    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }
}