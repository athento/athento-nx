package org.athento.nuxeo.security.api.descriptor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

import java.util.ArrayList;
import java.util.List;

@XObject(value = "ace")
public class ACEDescriptor {

    @XNode("@principal")
    protected String principal;

    @XNode("@complexField")
    protected String field;

    @XNode("@complexPattern")
    protected String pattern;

    @XNode("@permission")
    protected String permission = SecurityConstants.READ;

    @XNodeList(value = "rule", type = ArrayList.class, componentType = String.class)
    public final List<String> rules = new ArrayList<>();

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public List<String> getRules() {
        return rules;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}