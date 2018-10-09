package org.athento.nuxeo.security.api.descriptor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import java.util.ArrayList;
import java.util.List;

@XObject("dynamicAcl")
public class DynamicACLDescriptor {

    @XNode("@name")
    public String name = "";

    @XNode("@acl")
    public String acl = "local";

    @XNode("@overwrite")
    public boolean overwrite = true;

    @XNode("@blockInheritance")
    public boolean blockInheritance = false;

    @XNode("@enabled")
    public boolean enabled = true;

    @XNodeList(value = "doctype", type = ArrayList.class, componentType = String.class)
    public final List<String> doctypes = new ArrayList<>();

    @XNodeList(value = "facet", type = ArrayList.class, componentType = String.class)
    public final List<String> facets = new ArrayList<>();

    @XNodeList(value = "ace", type = ArrayList.class, componentType = ACEDescriptor.class)
    public final List<ACEDescriptor> aces = new ArrayList<>();

    /**
     * Check if dynamic acl contains a doctype.
     *
     * @param doctype
     * @return
     */
    public boolean hasDoctype(String doctype) {
        return doctypes.contains(doctype);
    }

}
