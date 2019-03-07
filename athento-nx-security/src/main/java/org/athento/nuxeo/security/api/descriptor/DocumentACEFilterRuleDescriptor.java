package org.athento.nuxeo.security.api.descriptor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("rule")
public class DocumentACEFilterRuleDescriptor {

    @XNode(value = "@grant")
    public boolean grant = false; // DENY

    @XNodeList(value = "username", type = String[].class, componentType = String.class)
    public String[] usernames;

    @XNodeList(value = "group", type = String[].class, componentType = String.class)
    public String[] groups;

    public String[] expressions;

    @XNodeList(value = "expressions", type = String[].class, componentType = String.class)
    public void setExpressions(String[] expressions) {
        this.expressions = expressions;
    }

    public DocumentACEFilterRuleDescriptor() {

    }


}
