package org.athento.nuxeo.api;

import org.nuxeo.ecm.automation.core.util.Paginable;

import java.io.Serializable;
import java.util.Map;

public interface PaginableEntrySet extends Paginable<Map<String, Serializable>>, EntrySet {

}
