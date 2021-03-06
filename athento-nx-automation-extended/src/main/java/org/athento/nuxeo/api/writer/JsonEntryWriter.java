package org.athento.nuxeo.api.writer;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.api.EntrySet;
import org.nuxeo.ecm.automation.jaxrs.io.JsonRecordSetWriter;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Manage JSON Marshalling for EntrySet.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JsonEntryWriter implements MessageBodyWriter<EntrySet> {

    protected static Log log = LogFactory.getLog(JsonRecordSetWriter.class);

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return EntrySet.class.equals(type);
    }

    @Override
    public long getSize(EntrySet t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1L;
    }

    @Override
    public void writeTo(EntrySet entries, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream out) throws WebApplicationException {
        try {
            writeEntries(out, entries);
        } catch (IOException e) {
            log.error("Failed to serialize entries", e);
            throw new WebApplicationException(500);
        }
    }

    protected void writeEntries(OutputStream out, EntrySet entries) throws IOException {
        JsonFactory factory = new JsonFactory();
        JsonGenerator jg = factory.createJsonGenerator(out, JsonEncoding.UTF8);
        jg.writeStartObject();
        jg.writeStartArray();
        for (Map<String, Serializable> entry : entries) {
            jg.writeObject(entry);
        }
        jg.writeEndArray();
        jg.writeEndObject();
        jg.flush();
    }

}
