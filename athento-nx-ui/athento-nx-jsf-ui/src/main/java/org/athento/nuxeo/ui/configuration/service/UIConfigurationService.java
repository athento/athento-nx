package org.athento.nuxeo.ui.configuration.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.io.Serializable;
import java.util.*;

/**
 * UIConfiguration Service.
 */
public class UIConfigurationService extends DefaultComponent implements UIConfigurationManager {

    private static final Log LOG = LogFactory.getLog(UIConfigurationService.class);

    /**
     * Query for get unique document config.
     */
    public static final String DOC_CONFIG_UNIQUE_QUERY = "SELECT * FROM ContentViewConfig";

    private static DocumentModel configUIDoc;

    /**
     * Save content view columsn of an user.
     *
     * @param session
     * @param contentView
     * @param columns
     * @param user
     */
    @Override
    public void saveContentViewColumns(final CoreSession session, final String contentView, final String[] columns, final String user) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Save content view columns for " + user);
        }
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                // Get document with ui-configuration
                loadUIConfigurationDocument(session);

                DocumentModel configDoc = session.getDocument(configUIDoc.getRef());

                // Columns configuration
                List<Map<String, Serializable>> allColumnsConfiguration =
                        (List<Map<String, Serializable>>) configDoc.getProperty("cvconfig", "contentView");


                // Get columns config for the user
                Map<String, Serializable> columnsConfig = getUserConfiguration(session, configDoc, user, contentView);

                if (columnsConfig == null) {
                    // Add new columns configuration for the user
                    columnsConfig = new HashMap<>();
                    columnsConfig.put("username", user);
                    columnsConfig.put("contentViewName", contentView);
                    columnsConfig.put("columnNames", columns);
                    allColumnsConfiguration.add(columnsConfig);
                } else {
                    // Remove old columns
                    removeColumnsForUserAndContentView(allColumnsConfiguration, user, contentView);
                    // Update columns for the configuration
                    columnsConfig.put("columnNames", columns);
                    allColumnsConfiguration.add(columnsConfig);
                }

                configDoc.setProperty("cvconfig", "contentView", allColumnsConfiguration);

                // Save document
                session.saveDocument(configDoc);
            }
        }.runUnrestricted();

        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

    }

    /**
     * Remove columns configuration for user and contentview.
     *
     * @param allColumnsConfiguration
     * @param user
     * @param contentViewName
     */
    private void removeColumnsForUserAndContentView(List<Map<String, Serializable>> allColumnsConfiguration, String user, String contentViewName) {
        for (Iterator<Map<String, Serializable>> it = allColumnsConfiguration.iterator(); it.hasNext(); ) {
            Map<String, Serializable> configItem = it.next();
            String username = (String) configItem.get("username");
            String contentView = (String) configItem.get("contentViewName");
            if (username.equals(user) && contentView.equals(contentViewName)) {
                it.remove();
            }
        }
    }

    /**
     * Get UI configuration document.
     *
     * @param session
     * @return
     */
    private void loadUIConfigurationDocument(CoreSession session) {
        if (UIConfigurationService.configUIDoc == null) {
            new UnrestrictedSessionRunner(session) {
                @Override
                public void run() {
                    DocumentModel doc = null;
                    // Get configuration-ui document
                    DocumentModelList configUIDocList = session.query(DOC_CONFIG_UNIQUE_QUERY);
                    if (configUIDocList.isEmpty()) {
                        // Create document with config-ui
                        doc = createConfigUIDoc(session);
                    } else {
                        doc = configUIDocList.get(0);
                    }
                    UIConfigurationService.configUIDoc = doc;
                    LOG.info("Document UI configuration id " + UIConfigurationService.configUIDoc.getId());
                }
            }.runUnrestricted();
        }
    }

    /**
     * Create config-ui doc.
     */
    private DocumentModel createConfigUIDoc(CoreSession session) {
        DocumentModel doc = session.createDocumentModel("/", "doc-ui-config", "ContentViewConfig");
        doc = session.createDocument(doc);
        session.saveDocument(doc);
        return doc;
    }

    /**
     * Get user configuration.
     *
     * @param configUIDoc
     * @param user
     * @param contentViewName
     * @return
     */
    private Map<String, Serializable> getUserConfiguration(CoreSession session, final DocumentModel configUIDoc, final String user, final String contentViewName) {
        final List<Map> items = new ArrayList<>();
        // Get content view columns
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                List<Map<String, Serializable>> configColumns =
                        (List<Map<String, Serializable>>) configUIDoc.getProperty("cvconfig", "contentView");
                for (Map<String, Serializable> configItem : configColumns) {
                    String username = (String) configItem.get("username");
                    String contentView = (String) configItem.get("contentViewName");
                    if (username.equals(user) && contentView.equals(contentViewName)) {
                        items.add(configItem);
                    }
                }
            }
        }.runUnrestricted();
        if (!items.isEmpty()) {
            return items.get(0);
        } else {
            return null;
        }
    }

    /**
     * Get columns of content view for an user.
     *
     * @param session
     * @param contentView
     * @param user
     * @return
     */
    @Override
    public String[] getContentViewColumns(CoreSession session, final String contentView, final String user) {
        final List<String []> columnNames = new ArrayList<>();
        // Get configuration-ui document
        loadUIConfigurationDocument(session);
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                DocumentModel configDoc = session.getDocument(configUIDoc.getRef());
                // Return columns
                Map<String, Serializable> columnsConfig = getUserConfiguration(session, configDoc, user, contentView);
                if (columnsConfig != null) {
                    columnNames.add((String []) columnsConfig.get("columnNames"));
                }
            }
        }.runUnrestricted();
        if (columnNames.size() > 0) {
            return columnNames.get(0);
        } else {
            return new String[0];
        }
    }


}
