package org.athento.nuxeo.security.api.descriptor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("schema")
public class DocumentACESchemaDescriptor {

    @XNode("@name")
    public String name = "";

    @XNodeList(value = "rule", type = String[].class, componentType = DocumentACEFilterRuleDescriptor.class)
    public DocumentACEFilterRuleDescriptor[] rules;

}
