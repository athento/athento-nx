package org.athento.nx.upgrade.stats;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Athento Stats Service.
 */
public interface AthentoStatsService {

    void startCalculation(String repositoryName, String [] doctypes, String path);

    void startCalculation(CoreSession session, String stat, String statName, String path);

    void calculateDocumentTypeTotals(CoreSession session, String[] doctypes, String path);

    void calculateUsersAndGroupsTotals(CoreSession session);

    void calculateDiskTotals(CoreSession session);

    boolean isCalculationRunning();

    void reindexStat(CoreSession session, String statName, boolean forceAll, int batchSize, boolean refresh);

}
