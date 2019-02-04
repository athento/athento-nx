package org.athento.nuxeo.module;

import org.athento.nuxeo.api.writer.JsonBatchResultWriter;
import org.athento.nuxeo.api.writer.JsonEntryWriter;
import org.nuxeo.ecm.automation.jaxrs.io.operations.MultiPartFormRequestReader;
import org.nuxeo.ecm.automation.jaxrs.io.operations.MultiPartRequestReader;
import org.nuxeo.ecm.webengine.app.WebEngineModule;

import java.util.HashSet;
import java.util.Set;

/**
 * Extended Module.
 *
 * @author <a href="vs@athento.com">Victor Sanchez</a>
 */
public class ExtendedModule extends WebEngineModule {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = super.getClasses();
        result.add(MultiPartRequestReader.class);
        result.add(MultiPartFormRequestReader.class);
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new HashSet<Object>();
        result.add(new JsonBatchResultWriter());
        result.add(new JsonEntryWriter());
        return result;
    }

}
