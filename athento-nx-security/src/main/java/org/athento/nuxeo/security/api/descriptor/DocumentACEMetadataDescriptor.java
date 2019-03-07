package org.athento.nuxeo.security.api.descriptor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("metadata")
public class DocumentACEMetadataDescriptor {

    @XNode("@xpath")
    public String xpath = "";

    @XNodeList(value = "rule", type = String[].class, componentType = DocumentACEFilterRuleDescriptor.class)
    public DocumentACEFilterRuleDescriptor[] rules;


}
