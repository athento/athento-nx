package org.athento.nx.upgrade.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.yerbabuena.athento.common.tree.Tree;
import org.yerbabuena.athento.common.tree.TreeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by victorsanchez on 10/9/16.
 */
public final class UpgradeUtils {

    private static final Log LOG = LogFactory.getLog(UpgradeUtils.class);

    /**
     * Export tree to JSON.
     *
     * @param tree is the tree
     * @throws JSONException on error
     */
    public static final <T> JSONObject treeToJson(Tree<T> tree) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        treeNodeToJson(tree.getRootNode(), jsonObject);
        return jsonObject;
    }

    /**
     * TreeNode to JSON.
     *
     * @param node
     * @param json
     * @param <T>
     * @throws JSONException
     */
    private static <T> void treeNodeToJson(TreeNode<T> node, JSONObject json) throws JSONException {
        json.put("node", node.getData());
        if (node.getNodes().size() > 0) {
            Collection<JSONObject> children = new ArrayList<JSONObject>();
            for (TreeNode<T> n : node.getNodes()) {
                JSONObject obj = new JSONObject();
                treeNodeToJson(n, obj);
                children.add(obj);
            }
            json.put("children", new JSONArray(children));
        }
    }

    /**
     * Run operation.
     *
     * @param operationId
     * @param input
     * @param params
     * @param session
     * @return
     * @throws Exception
     */
    public static Object runOperation(String operationId, Object input,
                                      Map<String, Object> params, CoreSession session) throws Exception {
        AutomationService automationManager = Framework
                .getLocalService(AutomationService.class);
        // Input setting
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(input);
        Object o = null;
        // Setting parameters of the chain
        try {
            // Run Automation service
            o = automationManager.run(ctx, operationId, params);
        } catch (Exception e) {
            throw e;
        }
        return o;
    }

}
