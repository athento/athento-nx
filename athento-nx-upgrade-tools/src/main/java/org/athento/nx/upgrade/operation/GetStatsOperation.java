package org.athento.nx.upgrade.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.stats.AthentoStatsService;
import org.athento.nx.upgrade.stats.RegisterStatInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.io.StringWriter;
import java.text.NumberFormat;

/**
 * Calculate stats.
 */
@Operation(id = GetStatsOperation.ID, category = "Athento", label = "Get statistics", description = "Get statistics")
public class GetStatsOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(GetStatsOperation.class);

    /** Operation ID. */
    public static final String ID = "Athento.GetStats";

    /** Run. */
    @OperationMethod
    public String run() throws Exception {
        AthentoStatsService service = Framework.getService(AthentoStatsService.class);
        if (service.isCalculationRunning()) {
            return "Waiting OK";
        } else {
            return toJSON();
        }
    }

    public String toJSON() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        ObjectMapper mapper = new ObjectMapper();
        StringWriter writer = new StringWriter();
        try {
            mapper.writeValue(writer, RegisterStatInfo.getInstance().getStats());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        return writer.toString();
    }

}
