package org.athento.metadatapropagation.utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Metadata utils.
 * 
 * @author victorsanchez
 * 
 */
public final class MetadataUtils {

	private static final Log LOG = LogFactory.getLog(MetadataUtils.class);

	/**
	 * Copy metadata from document source to destiny.
	 * 
	 * @param source
	 * @param destiny
	 * @param schemas
	 *            into the source document to propagate to destiny.
	 * @return destiny modified
	 */
	public static final DocumentModel copyMetadatas(DocumentModel source,
			DocumentModel destiny, String... schemas) {

		if (source == null) {
			throw new IllegalArgumentException(
					"Source document model must be not null");
		}
		if (destiny == null) {
			throw new IllegalArgumentException(
					"Destiny document model must be not null");
		}
		for (String schema : schemas) {
			Map<String, Object> properties = source.getProperties(schema);
			for (Map.Entry<String, Object> entry : properties.entrySet()) {
				String metadata = getMetadata(entry.getKey());
				Map<String, Object> destinyProperty = findProperty(destiny,
						metadata);
				if (!destinyProperty.isEmpty()) {
					destiny.setPropertyValue((String) destinyProperty
							.get("key"), (Serializable) source
							.getPropertyValue(entry.getKey()));
					LOG.info("Document '" + destiny.getId()
							+ "', has taken source property '" + metadata + "' with value '" +
							source
									.getPropertyValue(entry.getKey()) + "' does not have the property " + metadata);
				} else {
					LOG.info("Document '" + destiny.getId()
							+ "' does not have the property " + metadata);
				}
			}
		}

		return destiny;

	}

	/**
	 * Get metadata.
	 * 
	 * @param property
	 * @return
	 */
	private static String getMetadata(String property) {
		if (property.contains(":")) {
			return property.split(":")[1];
		} else {
			return property;
		}
	}

	/**
	 * Find a property into document model, in any schema that it has.
	 * 
	 * @param destiny
	 * @param propertyName
	 * @return property or null
	 */
	private static Map<String, Object> findProperty(DocumentModel doc,
			String propertyName) {
		Map<String, Object> property = new HashMap<String, Object>(2);
		String schemas[] = doc.getSchemas();
		for (String schema : schemas) {
			Map<String, Object> properties = doc.getProperties(schema);
			for (Map.Entry<String, Object> entry : properties.entrySet()) {
				if (getMetadata(entry.getKey()).equals(propertyName)) {
					property.put("key", entry.getKey());
					property.put("value", entry.getValue());
				}
			}
		}
		return property;
	}

}
