package org.athento.nuxeo.wf.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Upgrade information for a GraphNode.
 */
public class UpgradeInfo implements Serializable {

    List<String> openFrom;

    public UpgradeInfo(String upgradeInfo) {
        extractExpressions(upgradeInfo);
    }

    private void extractExpressions(String upgradeInfo) {
        String [] expressions = upgradeInfo.split(";");
        for (String expression : expressions) {
            String expr = expression.trim();
            if (expr.startsWith("openFrom:")) {
                expr = expr.replace("openFrom:", "");
                openFrom = Arrays.asList(expr.split(","));
            }
        }
    }

    public List<String> getOpenFrom() {
        return openFrom;
    }
}
