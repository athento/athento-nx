package org.athento.nuxeo.security.core;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.security.api.DynamicACLService;
import org.athento.nuxeo.security.api.descriptor.ACEDescriptor;
import org.athento.nuxeo.security.api.descriptor.ACLIgnoreDoctypeDescriptor;
import org.athento.nuxeo.security.api.descriptor.DynamicACLDescriptor;
import org.athento.nuxeo.security.util.MvelExpression;
import org.athento.nuxeo.security.util.MvelTemplate;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import java.util.*;


/**
 * Dynamic ACL service.
 */
public class DynamicACLServiceImpl extends DefaultComponent implements DynamicACLService {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(DynamicACLServiceImpl.class);

    public static final String DYNAMIC_ACL_EP = "dynamicAcl";
    public static final String IGNORE_ACL_DOCTYPE_EP = "ignoreAclDoctype";

    /** All policies registered. */
    public Map<String, DynamicACLDescriptor> dynamicACLs = new HashMap<>();
    public Map<String, List<DynamicACLDescriptor>> dynamicACLsByDoctype = new HashMap<>();
    public Map<String, List<DynamicACLDescriptor>> dynamicACLsByFacet = new HashMap<>();
    public List<String> ignoreDoctypes = new ArrayList<>();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (DYNAMIC_ACL_EP.equals(extensionPoint)) {
            if (!(contribution instanceof DynamicACLDescriptor)) {
                LOG.error("Contribution problem for " + contribution);
                return;
            }
            DynamicACLDescriptor desc = (DynamicACLDescriptor) contribution;
            addDynamicACL(desc);
        } else if (IGNORE_ACL_DOCTYPE_EP.equals(extensionPoint)) {
            if (!(contribution instanceof ACLIgnoreDoctypeDescriptor)) {
                return;
            }
            ACLIgnoreDoctypeDescriptor desc = (ACLIgnoreDoctypeDescriptor) contribution;
            if (!ignoreDoctypes.contains(desc.id)) {
                ignoreDoctypes.add(desc.id);
            }
            LOG.debug("Adding to ignore types for DynamicACL: " + desc.id);
        } else {
            LOG.error("Contribution extension problem for DynamicACL. Please check: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (DYNAMIC_ACL_EP.equals(extensionPoint)) {
            if (!(contribution instanceof DynamicACLDescriptor)) {
                return;
            }
            DynamicACLDescriptor desc = (DynamicACLDescriptor) contribution;
            removeDynamicACL(desc);
        } else if (IGNORE_ACL_DOCTYPE_EP.equals(extensionPoint)) {
            if (!(contribution instanceof ACLIgnoreDoctypeDescriptor)) {
                return;
            }
            ACLIgnoreDoctypeDescriptor desc = (ACLIgnoreDoctypeDescriptor) contribution;
            ignoreDoctypes.remove(desc.id);
        }
    }

    /**
     * Add dynamic ACL.
     *
     * @param desc
     */
    protected void addDynamicACL(DynamicACLDescriptor desc) {
        if (dynamicACLs.containsKey(desc.name)) {
            DynamicACLDescriptor dynamicACLDescriptor = dynamicACLs.get(desc.name);
            dynamicACLDescriptor.enabled = desc.enabled;
            dynamicACLDescriptor.overwrite = desc.overwrite;
        } else {
            dynamicACLs.put(desc.name, desc);
        }
        refreshDynamicACLsByDoctype();
        refreshDynamicACLsByFacet();
    }

    /**
     * Refresh dynamic ACLs by doctype.
     *
     */
    private void refreshDynamicACLsByDoctype() {
        dynamicACLsByDoctype.clear();
        for (Map.Entry<String, DynamicACLDescriptor> entry : dynamicACLs.entrySet()) {
            DynamicACLDescriptor acl = entry.getValue();
            if (acl.enabled) {
                for (String doctype : acl.doctypes) {
                    List<DynamicACLDescriptor> aclsByDoctype = dynamicACLsByDoctype.get(doctype);
                    if (aclsByDoctype == null) {
                        aclsByDoctype = new ArrayList<>();
                        dynamicACLsByDoctype.put(doctype, aclsByDoctype);
                    }
                    aclsByDoctype.add(acl);
                }
            }
        }
    }

    /**
     * Refresh dynamic ACLs by facet.
     *
     */
    private void refreshDynamicACLsByFacet() {
        dynamicACLsByFacet.clear();
        for (Map.Entry<String, DynamicACLDescriptor> entry : dynamicACLs.entrySet()) {
            DynamicACLDescriptor acl = entry.getValue();
            if (acl.enabled) {
                for (String facet : acl.facets) {
                    List<DynamicACLDescriptor> aclsByFacet = dynamicACLsByFacet.get(facet);
                    if (aclsByFacet == null) {
                        aclsByFacet = new ArrayList<>();
                        dynamicACLsByFacet.put(facet, aclsByFacet);
                    }
                    aclsByFacet.add(acl);
                }
            }
        }
    }

    /**
     * Remove dynamic ACL.
     *
     * @param desc
     */
    protected void removeDynamicACL(DynamicACLDescriptor desc) {
        dynamicACLs.remove(desc.name);
        refreshDynamicACLsByDoctype();
        refreshDynamicACLsByFacet();
    }

    /**
     * Get dynamic ACLs for a doctype.
     *
     * @param doc
     * @return
     */
    @Override
    public List<DynamicACLDescriptor> getDynamicACLsForDoctype(DocumentModel doc) {
        String type = doc.getDocumentType().getName();
        List<DynamicACLDescriptor> dynamicACLDescriptors = dynamicACLsByDoctype.get(type);
        if (dynamicACLDescriptors == null) {
            return Collections.emptyList();
        } else {
            return dynamicACLDescriptors;
        }
    }

    /**
     * Get dynamic ACLs for a facet.
     *
     * @param facet
     * @return
     */
    @Override
    public List<DynamicACLDescriptor> getDynamicACLsForFacet(String facet) {
        List<DynamicACLDescriptor> dynamicACLDescriptors = dynamicACLsByFacet.get(facet);
        if (dynamicACLDescriptors == null) {
            return Collections.emptyList();
        } else {
            return dynamicACLDescriptors;
        }
    }

    /**
     * Check if a doctype is ignored for Dynamic ACLs.
     *
     * @param doctype
     * @return
     */
    @Override
    public boolean isIgnoredDoctype(String doctype) {
        return ignoreDoctypes.contains(doctype);
    }

    /**
     * Get enabled ACEs for a document.
     *
     * @param dynamicACL
     * @param doc
     * @return
     */
    @Override
    public List<ACE> getACEsForDocument(DynamicACLDescriptor dynamicACL, DocumentModel doc) {
        List<ACE> resultACEs = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("doc", doc);
        params.put("document", doc);
        params.put("this", doc);
        for (ACEDescriptor aced : dynamicACL.aces) {
            Object result = getExpressionValue(aced.getPrincipal(), params);
            if (result != null) {
                if (result instanceof Collection) {
                    includeACEsOfCollection(doc, aced, result, resultACEs);
                } else {
                    params.put("principal", result);
                    if (checkRules(aced, params)) {
                        String username = result.toString();
                        if (username != null && !username.isEmpty() && !"null".equals(username)) {
                            ACE ace = new ACE(username, aced.getPermission(), true);
                            resultACEs.add(ace);
                        }
                    }
                }
            }
        }
        return resultACEs;
    }

    /**
     * Get ACEs from collection.
     *
     * @param aced
     * @param result
     * @return
     */
    private void includeACEsOfCollection(DocumentModel doc, ACEDescriptor aced, Object result, List<ACE> aces) {
        Collection list = (Collection) result;
        for (Object value : list) {
            if (value == null) {
                continue;
            }
            Map<String, Object> params = new HashMap<>();
            params.put("doc", doc);
            params.put("document", doc);
            params.put("this", doc);
            if (value instanceof Map) {
                String complexField = aced.getField();
                if (complexField != null) {
                    Object complexValue = ((Map) value).get(complexField);
                    if (complexValue != null) {
                        if (complexValue instanceof Collection) {
                            includeACEsOfCollection(doc, aced, complexValue, aces);
                        } else {
                            if (aced.getPattern() != null) {
                                params.put("complexValue", complexValue.toString());
                                complexValue = aced.getPattern().replace("%", complexValue.toString());
                            }
                            params.put("principal", complexValue);
                            if (checkRules(aced, params)) {
                                String username = complexValue.toString();
                                if (username != null && !username.isEmpty() && !"null".equals(username)) {
                                    aces.add(new ACE(username, aced.getPermission(), true));
                                }
                            }
                        }
                    }
                }
            } else {
                if (aced.getPattern() != null) {
                    params.put("value", value.toString());
                    value = aced.getPattern().replace("%", value.toString());
                }
                params.put("principal", value);
                if (checkRules(aced, params)) {
                    String username = value.toString();
                    if (username != null && !username.isEmpty() && !"null".equals(username)) {
                        aces.add(new ACE(username, aced.getPermission(), true));
                    }
                }
            }
        }
    }

    /**
     * Get expression value.
     *
     * @param expr
     * @param params
     * @return
     */
    private Object getExpressionValue(String expr, Map<String, Object> params) {
        if (expr.startsWith("expr:")) {
            expr = expr.substring(5);
            try {
                if (expr.contains("@{")) {
                    MvelTemplate mvel = new MvelTemplate(expr);
                    return mvel.eval(params);
                } else {
                    MvelExpression mvel = new MvelExpression(expr);
                    return mvel.eval(params);
                }
            } catch (Exception e) {
                return null;
            }
        } else {
            return expr;
        }
    }


    /**
     * Check rules to apply dynamic acl.
     *
     * @param ace
     * @param params
     * @return
     */
    protected final boolean checkRules(ACEDescriptor ace, Map<String, Object> params) {
        if (ace.getRules().isEmpty()) {
            return true;
        }
        for (String condition : ace.getRules()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Condition to check: " + condition);
            }
            MvelExpression mvelExpression = new MvelExpression(condition);
            Object result = mvelExpression.eval(params);
            if (result instanceof Boolean && (Boolean) result) {
                return true;
            }
        }
        return false;
    }

}
