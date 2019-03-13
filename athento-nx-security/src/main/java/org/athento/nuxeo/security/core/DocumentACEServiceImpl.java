package org.athento.nuxeo.security.core;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.security.api.DocumentACEResult;
import org.athento.nuxeo.security.api.DocumentACEService;
import org.athento.nuxeo.security.api.descriptor.DocumentACEDescriptor;
import org.athento.nuxeo.security.api.descriptor.DocumentACEFilterRuleDescriptor;
import org.athento.nuxeo.security.api.descriptor.DocumentACEMetadataDescriptor;
import org.athento.nuxeo.security.api.descriptor.DocumentACESchemaDescriptor;
import org.athento.nuxeo.security.util.MvelExpression;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Document ACE service.
 */
public class DocumentACEServiceImpl extends DefaultComponent implements DocumentACEService {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(DocumentACEServiceImpl.class);

    public static final String DOCUMENTACE_EP = "documentACE";

    public Map<String, DocumentACEDescriptor> documentACEs = new HashMap<>();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (DOCUMENTACE_EP.equals(extensionPoint)) {
            if (!(contribution instanceof DocumentACEDescriptor)) {
                LOG.error("Contribution problem for " + contribution);
                return;
            }
            DocumentACEDescriptor desc = (DocumentACEDescriptor) contribution;
            addDocumentACE(desc);
        } else {
            LOG.error("Contribution extension problem for DocumentACE. Please check: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (DOCUMENTACE_EP.equals(extensionPoint)) {
            if (!(contribution instanceof DocumentACEDescriptor)) {
                return;
            }
            DocumentACEDescriptor desc = (DocumentACEDescriptor) contribution;
            removeDocumentACE(desc);
        }
    }

    /**
     * Add document ACE.
     *
     * @param desc
     */
    protected void addDocumentACE(DocumentACEDescriptor desc) {
        if (documentACEs.containsKey(desc.doctype)) {
            // Merge
            DocumentACEDescriptor dynamicACLDescriptor = documentACEs.get(desc.doctype);
            dynamicACLDescriptor.enabled = desc.enabled;
            dynamicACLDescriptor.metadatas.addAll(desc.metadatas);
            dynamicACLDescriptor.schemas.addAll(desc.schemas);
        } else {
            documentACEs.put(desc.doctype, desc);
        }
    }

    /**
     * Remove document ACE.
     *
     * @param desc
     */
    protected void removeDocumentACE(DocumentACEDescriptor desc) {
        documentACEs.remove(desc.doctype);
    }

    /**
     * Check document ACEs.
     *
     * @param doc
     * @param principal
     * @return
     */
    @Override
    public DocumentACEResult checkDocumentACEs(DocumentModel doc, Principal principal) {
        DocumentACEResult result = new DocumentACEResult();
        DocumentACEDescriptor doctypeACE = getDocumentACE(doc.getType());
        if (doctypeACE != null) {
            // Check schemas
            List<String> schemaErrors = checkSchemas(doc, doctypeACE, principal);
            if (!schemaErrors.isEmpty()) {
                result.getErrors().addAll(schemaErrors);
            }
            // Check metadatas
            List<String> metadataErrors = checkMetadatas(doc, doctypeACE, principal);
            if (!metadataErrors.isEmpty()) {
                result.getErrors().addAll(metadataErrors);
            }
        }
        return result;
    }

    /**
     * Check schemas.
     *
     * @param doc
     * @param descriptor
     * @param principal
     * @return list of errors
     */
    private List<String> checkSchemas(DocumentModel doc, DocumentACEDescriptor descriptor, Principal principal) {
        List<String> errors = new ArrayList<>();
        List<DocumentACESchemaDescriptor> schemas = descriptor.schemas;
        for (DocumentACESchemaDescriptor schema : schemas) {
            if (schema.rules.length > 0) {
                if (doc.hasSchema(schema.name)) {
                    Map<String, Object> schemaValues = doc.getProperties(schema.name);
                    for (Object value : schemaValues.values()) {
                        if (value != null) {
                            if (!checkRules(schema.rules, doc, principal)) {
                                errors.add("Schema " + schema.name + " is not allowed");
                            }
                        }
                    }
                } else {
                    LOG.warn("Schema " + schema.name + " is not found to check for DocumentACE");
                }
            }
        }
        return errors;
    }

    /**
     * Check metadatas.
     *
     * @param doc
     * @param descriptor
     * @param principal
     * @return list of errors
     */
    private List<String> checkMetadatas(DocumentModel doc, DocumentACEDescriptor descriptor, Principal principal) {
        List<String> errors = new ArrayList<>();
        List<DocumentACEMetadataDescriptor> metadatas = descriptor.metadatas;
        for (DocumentACEMetadataDescriptor metadata : metadatas) {
            try {
                Serializable value = doc.getPropertyValue(metadata.xpath);
                if (value != null) {
                    if (metadata.rules.length > 0) {
                        if (!checkRules(metadata.rules, doc, principal)) {
                            errors.add("Metadata " + metadata.xpath + " is not allowed");
                        }
                    }
                }
            } catch (PropertyNotFoundException e) {
                LOG.warn("Metadata " + metadata.xpath + " is not found to check for DocumentACE");
            }
        }
        return errors;
    }

    /**
     * Check rules.
     *
     * @param rules
     * @param doc
     * @param principal
     * @return
     */
    private Boolean checkRules(DocumentACEFilterRuleDescriptor[] rules, DocumentModel doc, Principal principal) {
        List<String> groups = ((NuxeoPrincipal) principal).getGroups();
        Map<String, Object> params = new HashMap<>();
        params.put("doc", doc);
        params.put("document", doc);
        params.put("this", doc);
        params.put("principal", principal);
        for (DocumentACEFilterRuleDescriptor rule : rules) {
            if (rule.usernames.length > 0) {
                for (String username : rule.usernames) {
                    if (principal.getName().equals(username)) {
                        return rule.grant;
                    }
                }
            }
            if (rule.groups.length > 0) {
                for (String group : rule.groups) {
                    if (groups.contains(group)) {
                        return rule.grant;
                    }
                }
            }
            if (rule.expressions.length > 0) {
                for (String expression : rule.expressions) {
                    if (checkExpression(expression, params)) {
                        return rule.grant;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public DocumentACEDescriptor getDocumentACE(String doctype) {
        return documentACEs.get(doctype);
    }

    /**
     * Check rules to apply document ACE.
     *
     * @param expression
     * @param params
     * @return
     */
    protected final boolean checkExpression(String expression, Map<String, Object> params) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Condition to check: " + expression);
        }
        MvelExpression mvelExpression = new MvelExpression(expression);
        Object result = mvelExpression.eval(params);
        if (result instanceof Boolean && (Boolean) result) {
            return true;
        }
        return false;
    }

}
