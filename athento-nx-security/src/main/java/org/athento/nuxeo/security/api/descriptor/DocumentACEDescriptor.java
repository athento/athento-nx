package org.athento.nuxeo.security.api.descriptor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import java.util.ArrayList;
import java.util.List;

@XObject("documentACE")
public class DocumentACEDescriptor {

    @XNode("@doctype")
    public String doctype = "";

    @XNode("@enabled")
    public Boolean enabled = true;

    @XNodeList(value = "metadata", type = ArrayList.class, componentType = DocumentACEMetadataDescriptor.class)
    public List<DocumentACEMetadataDescriptor> metadatas = new ArrayList<>();

    @XNodeList(value = "schema", type = ArrayList.class, componentType = DocumentACESchemaDescriptor.class)
    public List<DocumentACESchemaDescriptor> schemas = new ArrayList<>();

    public boolean isEnabled() {
        return enabled == null || Boolean.TRUE.equals(enabled);
    }

    @Override
    public DocumentACEDescriptor clone() {
        DocumentACEDescriptor clone = new DocumentACEDescriptor();
        clone.doctype = doctype;
        clone.enabled = enabled;
        if (!metadatas.isEmpty()) {
            clone.metadatas = new ArrayList<>(metadatas);
        }
        if (!schemas.isEmpty()) {
            clone.schemas = new ArrayList<>(schemas);
        }
        return clone;
    }
}
