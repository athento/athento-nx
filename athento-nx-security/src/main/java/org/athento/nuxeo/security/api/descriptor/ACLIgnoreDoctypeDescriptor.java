package org.athento.nuxeo.security.api.descriptor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("ignoreAclDoctype")
public class ACLIgnoreDoctypeDescriptor {

    @XNode("@id")
    public String id = "";

}
