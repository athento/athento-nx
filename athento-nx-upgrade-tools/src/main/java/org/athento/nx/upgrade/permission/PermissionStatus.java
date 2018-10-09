package org.athento.nx.upgrade.permission;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.model.Document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by victorsanchez on 9/9/16.
 */
public final class PermissionStatus {

    DocumentModel doc;
    ACP acp;

    List<ACE> denied;
    List<ACE> granted;

    public PermissionStatus(DocumentModel doc) {
        this.doc = doc;
    }

    public List<ACE> getGranted() {
        if (granted == null) {
            granted = new ArrayList<>();
        }
        return granted;
    }

    public ACP getAcp() {
        return acp;
    }

    public void setAcp(ACP acp) {
        this.acp = acp;
    }

    public List<ACE> getDenied() {
        if (denied == null) {
            denied = new ArrayList<>();
        }
        return denied;
    }

    public boolean hasGranted(ACE ace) {
        for (ACE acei : getGranted()) {
            if (acei.getPermission().equals(ace.getPermission())
                    && acei.getUsername().equals(ace.getUsername())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasDenied(ACE ace) {
        for (ACE acei : getDenied()) {
            if (acei.getPermission().equals(ace.getPermission())
                    && acei.getUsername().equals(ace.getUsername())) {
                return true;
            }
        }
        return false;
    }

    public DocumentModel getDoc() {
        return doc;
    }

    public void setDoc(DocumentModel doc) {
        this.doc = doc;
    }

    /**
     * Remove denied ace.
     *
     * @param ace
     */
    public void removeDenied(ACE ace) {
        for (Iterator<ACE> it = getDenied().iterator(); it.hasNext(); ) {
            ACE acei = it.next();
            if (acei.getPermission().equals(ace.getPermission()) && acei.getUsername().equals(ace.getUsername())) {
                it.remove();
            }
        }
    }

    /**
     * Remove granted ace.
     *
     * @param ace
     */
    public void removeGranted(ACE ace) {
        for (Iterator<ACE> it = getGranted().iterator(); it.hasNext(); ) {
            ACE acei = it.next();
            if (acei.getPermission().equals(ace.getPermission()) && acei.getUsername().equals(ace.getUsername())) {
                it.remove();
            }
        }
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer("doc=" + doc.getName());
        for (ACL acl : acp.getACLs()) {
            result.append("acl=" + acl.getName() + ": [ ");
            int i = 0;
            for (ACE ace : acl.getACEs()) {
                result.append("ace=" + ace);
                if (i < acl.size() - 1) {
                    result.append(",");
                }
                i++;
            }
            result.append("]");
        }
        return result.toString();
    }
}
