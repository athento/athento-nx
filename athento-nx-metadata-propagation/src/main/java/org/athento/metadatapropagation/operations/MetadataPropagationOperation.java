package org.athento.metadatapropagation.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.metadatapropagation.utils.MetadataUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.platform.routing.core.api.TasksInfoWrapper;

/**
 * Metadata propagation operation.
 * 
 * @author victorsanchez
 * 
 */
@Operation(id = MetadataPropagationOperation.ID, category = Constants.CAT_DOCUMENT, label = "Propagate metadata", description = "Copy metadata between documents, from source schema to destiny with same metadata names between both one.")
public class MetadataPropagationOperation {

	private static final Log LOG = LogFactory
			.getLog(MetadataPropagationOperation.class);

	/** Operation ID. */
	public final static String ID = "Document.PropagateMetadatas";

	/** Session context. */
	@Context
	protected CoreSession session;

	/** Operation context. */
	@Context
	protected OperationContext ctxt;

	/**
	 * Run method of metadata propagation.
	 * 
	 * @throws Exception
	 *             on error
	 */
	@OperationMethod(collector = DocumentModelCollector.class)
	public DocumentModel run(DocumentModel doc) throws Exception {

		String taskDocId = (((TasksInfoWrapper) ((Map<?, ?>) ctxt
				.get("NodeVariables")).get("tasks")).get(0).getTaskDocId());

		DocumentModel taskDoc = session.getDocument(new IdRef(taskDocId));
		ListProperty taskProperties = (ListProperty) taskDoc
				.getProperty("nt:task_variables");

		String docId = (String) getPropertyValue(taskProperties,
				"document.routing.step");

		LOG.info("Routing doc " + docId);

		if (docId == null) {
			LOG.error("Document id of routing is null. Do nothing.");
			return doc;
		}

		DocumentModel taskDocumentModel = session.getDocument(new IdRef(docId));

		String varSchemas[] = getNodeSchema(taskDocumentModel);
		// Copy metadatas
		MetadataUtils.copyMetadatas(taskDocumentModel, doc, varSchemas);

		// Save doc
		session.saveDocument(doc);
		session.save();

		return doc;
	}

	/**
	 * Get schemas of node.
	 * 
	 * @param taskDocumentModel
	 * @return
	 */
	private String[] getNodeSchema(DocumentModel taskDocumentModel) {
		List<String> schemas = new ArrayList<String>();
		for (String schema : taskDocumentModel.getSchemas()) {

			if (schema.startsWith("var-")) {
				schemas.add(schema);
			}
		}
		return schemas.toArray(new String[0]);
	}

	/**
	 * Get property.
	 * 
	 * @param list
	 * @param property
	 * @return
	 */
	private Object getPropertyValue(ListProperty list, String property) {
		for (Property p : list) {
			Map<?, ?> value = (Map<?, ?>) p.getValue();
			if (value.get("key").equals(property)) {
				return value.get("value");
			}
		}
		return null;
	}
}
