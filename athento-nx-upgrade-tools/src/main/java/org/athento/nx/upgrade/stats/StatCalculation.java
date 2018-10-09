package org.athento.nx.upgrade.stats;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Stat calculation.
 */
public interface StatCalculation {

    /** Run calculation. */
    void runCompleteCalculation(CoreSession session);
}
