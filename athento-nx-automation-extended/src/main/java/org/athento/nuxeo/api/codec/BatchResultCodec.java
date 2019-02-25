
package org.athento.nuxeo.api.codec;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.athento.nuxeo.api.model.BatchResult;
import org.athento.nuxeo.api.writer.JsonBatchResultWriter;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodec;
import org.nuxeo.ecm.core.api.CoreSession;

import java.io.IOException;

/**
 * Code for BatchResult.
 */
public class BatchResultCodec<T extends BatchResult> extends ObjectCodec<T> {

    @Override
    public String getType() {
        return "batch-result";
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public void write(JsonGenerator jg, T value) {
        try {
            JsonBatchResultWriter.writeBatchResult(jg, value, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T read(JsonParser jp, CoreSession session) throws
            IOException {
        // FIXME:
        return null;
    }
}
