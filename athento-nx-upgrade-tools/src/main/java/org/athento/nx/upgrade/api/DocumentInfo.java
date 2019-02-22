package org.athento.nx.upgrade.api;

/**
 * Document information class.
 */
public final class DocumentInfo extends GenericInfo {

    String doctype;
    Long totalDocuments;
    Long indexedDocuments;
    String path;
    boolean all = false;

    public DocumentInfo(String name) {
        super(name);
        this.doctype = name;
    }

    public DocumentInfo(String name, String path) {
        super(name);
        this.path = path;
        this.doctype = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDoctype() {
        return doctype;
    }

    public void setDoctype(String doctype) {
        this.doctype = doctype;
    }

    public Long getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(Long totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public Long getIndexedDocuments() {
        return indexedDocuments;
    }

    public void setIndexedDocuments(Long indexedDocuments) {
        this.indexedDocuments = indexedDocuments;
    }

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    @Override
    public String toString() {
        return "DocumentInfo{" +
                "name='" + name + '\'' +
                ", doctype='" + doctype + '\'' +
                ", description='" + description + '\'' +
                ", totalDocuments=" + totalDocuments +
                ", indexedDocuments=" + indexedDocuments +
                ", all=" + all +
                '}';
    }
}
