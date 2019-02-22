package org.athento.nx.upgrade.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nx.upgrade.permission.PermissionStatus;
import org.athento.nx.upgrade.util.UpgradeUtils;
import org.json.JSONObject;
import org.nuxeo.ecm.automation.core.operations.document.DocumentPermissionHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;
import org.yerbabuena.athento.common.tree.SortedTreeNode;
import org.yerbabuena.athento.common.tree.Tree;
import org.yerbabuena.athento.common.tree.TreeNode;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

/**
 * Upgrade permission worker.
 *
 * @author victorsanchez
 */
public class UpgradePermissionWorker extends AbstractWork {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(UpgradePermissionWorker.class);

    /**
     * Title.
     */
    public static final String TITLE = "Upgrade permissions to 6.0+";

    /**
     * Category.
     */
    public static final String CATEGORY = "AthentoUpgradeTool";

    /**
     * Document root.
     */
    protected DocumentModel root;

    /**
     * ACL name.
     */
    protected String aclName;

    /**
     * Only folders.
     */
    protected boolean onlyFolders = true;

    /**
     * Save at end.
     */
    protected boolean save = false;

    /**
     * Updated document ACPs.
     */
    private Tree<PermissionStatus> workingTree = new Tree<>();

    /**
     * Current denied ACEs.
     */
    private List<ACE> currentDeniedACEs = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param doc     is the root document
     * @param aclName is the acl name
     */
    public UpgradePermissionWorker(DocumentModel doc, String aclName) {
        if (aclName == null) {
            throw new IllegalArgumentException("ACL is mandatory to start " +
                    "the upgrade permission procedure");
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting upgrade permission worker with " + doc.getId() + "(" + doc.getName() + ") and ACL: " + aclName);
        }
        this.root = doc;
        this.aclName = aclName;
    }

    /**
     * Get title of worker.
     *
     * @return title
     */
    @Override
    public String getTitle() {
        return getCategory();
    }

    /**
     * Get category.
     *
     * @return category
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /**
     * Work handler.
     *
     * @throws Exception on error
     */
    @Override
    public void work() {

        // Init session
        initSession();

        // Login context started
        try {
            Framework.login();

            // Get root node with all information
            TreeNode<PermissionStatus> rootNode = getRootNode();

            // Make document tree
            doSnapshotACPs(rootNode, null);

            // Check ACLs
            do {
                LOG.info("Checking again...");
                currentDeniedACEs.clear();
                LOG.info("Curren tree status " + workingTree);
                checkACLs(workingTree.getRootNode());
                LOG.info("current denied " + currentDeniedACEs.size());
            } while (currentDeniedACEs.size() > 0);

            // Update ACPs
            if (save) {
                updateACPs();
            } else {
                JSONObject json = UpgradeUtils.treeToJson(workingTree);
                try (FileWriter file = new FileWriter("/tmp/upgrade-" + Calendar.getInstance().getTimeInMillis())) {
                    file.write(json.toString());
                    LOG.info("Successfully saved JSON Object to file");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close session
            closeSession();
        }

    }

    /**
     * Get root node.
     *
     * @return
     */
    private TreeNode<PermissionStatus> getRootNode() {
        PermissionStatus rootStatus = new PermissionStatus(root);
        rootStatus.setAcp(root.getACP().clone());
        TreeNode<PermissionStatus> rootNode = new SortedTreeNode<>(root.getId(), rootStatus);
        for (ACL acli : rootStatus.getAcp().getACLs()) {
            for (ACE ace : acli.getACEs()) {
                // Add ACE to status
                addACEToStatus(rootNode.getData(), ace);
            }
        }
        workingTree.addNode(rootNode, null);
        return rootNode;
    }

    /**
     * Update cumulative ACPs.
     */
    private void updateACPs() throws Exception {
        for (TreeNode<PermissionStatus> node : workingTree.getProofNodes()) {
            DocumentModel doc = node.getData().getDoc();
            ACP acp = node.getData().getAcp();
            printPretty(doc, null, acp);
            // Check acp has not negative permission
            if (!hasNegativePermission(acp)) {
                LOG.info("Update ACP for document " + doc.getId() + "(" + doc.getName() + ")");
                doc.setACP(acp, true);
                session.saveDocument(doc);
            } else {
                LOG.warn("Unable to update ACP with negative " +
                        "permission into document " + doc.getId() + "(" + doc.getName() + ")");
            }
        }
    }

    /**
     * Check if ACP has negative permission.
     *
     * @param acp
     * @return
     */
    private boolean hasNegativePermission(ACP acp) {
        for (ACL acl : acp.getACLs()) {
            if (!"inherited".equals(acl.getName())) {
                for (ACE ace : acl.getACEs()) {
                    if (ace.isDenied()) {
                        LOG.warn("ACE " + ace + " is negative!");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Do first snapshot ACPs.
     *
     * @param node
     * @param parentNode
     */
    private void doSnapshotACPs(TreeNode<PermissionStatus> node, TreeNode<PermissionStatus> parentNode) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Snapshot ACPs for " + node.getData().getDoc().getId() + "(" + node.getData().getDoc().getName() + ")...");
        }
        DocumentModelList children = this.session.getChildren(node.getData().getDoc().getRef());
        if (!children.isEmpty()) {
            for (DocumentModel childDoc : children) {
                if (!onlyFolders || (onlyFolders && childDoc.isFolder())) {
                    // Add node
                    PermissionStatus status = new PermissionStatus(childDoc);
                    status.setAcp(childDoc.getACP().clone());
                    TreeNode<PermissionStatus> childNode = new SortedTreeNode<>(childDoc.getId(), status);
                    workingTree.addNode(childNode, node);
                    for (ACL acl : status.getAcp().getACLs()) {
                        for (ACE ace : acl.getACEs()) {
                            // Add ACE to status
                            addACEToStatus(node.getData(), ace);
                        }
                    }
                    // Recursive call
                    doSnapshotACPs(childNode, node);
                }
            }
        }
    }

    /**
     * Check ACLs recursive in proof.
     *
     * @param node is the node to check
     */
    private void checkACLs(TreeNode<PermissionStatus> node) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Checking ACLs for " + node.getData().getDoc().getId()
                    + "(" + node.getData().getDoc().getName() + ")...");
        }
        Collection<TreeNode<PermissionStatus>> nodes = node.getAllNodes();
        for (TreeNode<PermissionStatus> n : nodes) {
            DocumentModel doc = n.getData().getDoc();
            // Get ACL for document
            ACL acl = getACL(doc);
            if (acl != null) {
                ACE[] aces = acl.getACEs();
                for (ACE ace : aces) {
                    // Check if ACE is denied
                    if (ace.isDenied()) {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Detected denied ACE of " + doc.getId() + "(" + doc.getName() + ") " + ace.getUsername() + ", " + ace.getPermission());
                        }
                        // Add denied to current
                        currentDeniedACEs.add(ace);
                        // Upgrade ACE
                        upgradeACE(workingTree.getRootNode(), ace);
                    }
                }
            }
        }
    }

    /**
     * Add an ace to status.
     *
     * @param status
     * @param ace
     */
    private void addACEToStatus(PermissionStatus status, ACE ace) {
        if (ace.isDenied()) {
            if (!status.hasDenied(ace)) {
                status.getDenied().add(ace);
            }
        } else if (ace.isGranted()) {
            if (!status.hasGranted(ace)) {
                status.getGranted().add(ace);
            }
        }
    }

    /**
     * Upgrade node to leafs for denied ACE.
     *
     * @param node is the node
     * @param ace  is the ace to upgrade
     */
    private void upgradeACE(TreeNode<PermissionStatus> node, ACE ace) {
        DocumentModel doc = node.getData().getDoc();
        if (LOG.isInfoEnabled()) {
            LOG.info("Upgrading ACE of " + doc.getId() + "(" + doc.getName() + ") with ACE " + ace.getPermission() + ", " + ace.getUsername());
        }
        Collection<TreeNode<PermissionStatus>> children = node.getNodes();
        if (!children.isEmpty()) {
            for (TreeNode<PermissionStatus> childDoc : children) {
                if (!onlyFolders || (onlyFolders && childDoc.getData().getDoc().isFolder())) {
                    upgradeACE(childDoc, ace);
                }
            }
        }
        if (!hasGrantedACE(doc, ace)) {
            if (!hasDeniedACE(doc, ace)) {
                // Add ACE granted
                if (!doc.equals(root)) {
                    addACE(doc, ace);
                }
            } else {
                TreeNode<PermissionStatus> statusNode = workingTree.getTreeNode(doc.getId());
                PermissionStatus status = statusNode.getData();
                if (status != null && status.hasDenied(ace)) {
                    // Remove from denied
                    status.removeDenied(ace);
                    LOG.info("Add ACE denied " + ace.getUsername()
                            + " to " + doc.getId() + "(" + doc.getName() + ")");
                    addACE(doc, ace);
                } else {
                    LOG.info("Remove ACE granted and denied" + ace.getUsername()
                            + " to " + doc.getId() + "(" + doc.getName() + ")");
                    // Remove denied ACE
                    removeACE(doc, ace);
                }
            }
        } else {
            LOG.info("Remove ACE not granted" + ace.getUsername()
                    + " to " + doc.getId() + "(" + doc.getName() + ")");
            // Remove ACE
            removeACE(doc, ace);
        }
    }

    /**
     * Add ACE to current {@this aclName} of document.
     *
     * @param doc
     * @param ace
     */
    private void addACE(DocumentModel doc, ACE ace) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Adding ACE for " + doc.getId() + "(" + doc.getName() + ") " + ace.getPermission() + ", " + ace.getUsername());
        }
        ACP acp = workingTree.getTreeNode(doc.getId()).getData().getAcp();
        if (!containsACE(acp, aclName, ace)) {
            boolean permissionChanged = DocumentPermissionHelper.addPermission(acp, aclName, ace.getUsername(),
                    ace.getPermission(), false, session.getPrincipal().getName());
            if (permissionChanged) {
                TreeNode<PermissionStatus> statusNode = workingTree.getTreeNode(doc.getId());
                PermissionStatus status = statusNode.getData();
                status.setAcp(acp);
                if (ace.isGranted()) {
                    if (!statusNode.getData().hasGranted(ace)) {
                        // Add granted
                        statusNode.getData().getGranted().add(ace);
                    }
                } else if (ace.isDenied()) {
                    if (!statusNode.getData().hasDenied(ace)) {
                        // Add granted
                        statusNode.getData().getDenied().add(ace);
                    }
                }
                printPretty(doc, ace, acp);
            }
        }
    }

    /**
     * Check if an ACL contains an ACE.
     *
     * @param acp
     * @param aclName
     * @param ace
     * @return
     */
    private boolean containsACE(ACP acp, String aclName, ACE ace) {
        ACL acl = acp.getACL(aclName);
        if (acl == null) {
            return false;
        }
        for (ACE acei : acl.getACEs()) {
            if (acei.getUsername().equals(ace.getUsername()) && acei.getPermission().equals(ace.getPermission())) {
                return ((ace.isGranted() && acei.isGranted())
                        || (ace.isDenied() && acei.isDenied()));
            }
        }
        return false;
    }

    /**
     * Remove ACE of current {@this aclName}.
     *
     * @param doc
     * @param ace
     */
    private void removeACE(DocumentModel doc, ACE ace) {
        TreeNode<PermissionStatus> statusNode = workingTree.getTreeNode(doc.getId());
        ACP acp = statusNode.getData().getAcp();
        for (ACL acl : acp.getACLs()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Removing ACE for " + doc.getId() + "(" + doc.getName() + ") " + ace.getPermission() + ", " + ace.getUsername() + " in ACL: " + acl.getName());
            }
            boolean permissionChanged = DocumentPermissionHelper.removePermission(acp, acl.getName(), ace.getUsername());
            if (permissionChanged) {
                PermissionStatus status = statusNode.getData();
                status.setAcp(acp);
                printPretty(doc, ace, acp);
            }
            // Remove denied ACE
            statusNode.getData().removeDenied(ace);
        }
    }

    /**
     * Get ACL of {@this aclName} of document.
     *
     * @param doc
     * @return the ACL or null
     */
    private ACL getACL(DocumentModel doc) {
        return workingTree.getTreeNode(doc.getId()).getData().getAcp().getACL(aclName);
    }

    /**
     * Check if a document has denied ACE (in current ACL).
     *
     * @param doc
     * @param ace
     * @return true if ACE is denied.
     */
    private boolean hasDeniedACE(DocumentModel doc, ACE ace) {
        TreeNode<PermissionStatus> statusNode = workingTree.getTreeNode(doc.getId());
        for (ACL acl : statusNode.getData().getAcp().getACLs()) {
            for (ACE acei : acl.getACEs()) {
                if (acei.isDenied()
                        && acei.getPermission().equals(ace.getPermission())
                        && acei.getUsername().equals(ace.getUsername())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a document has granted ACE (in any ACL).
     *
     * @param doc
     * @param ace
     * @return true if ACE is granted.
     */
    private boolean hasGrantedACE(DocumentModel doc, ACE ace) {
        ACL acl = getACL(doc);
        if (acl == null) {
            return false;
        }
        for (ACE acei : acl.getACEs()) {
            if (acei.isGranted()
                    && acei.getPermission().equals(ace.getPermission())
                    && acei.getUsername().equals(ace.getUsername())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set only folders flag.
     *
     * @param onlyFolders
     */
    public void setOnlyFolders(boolean onlyFolders) {
        this.onlyFolders = onlyFolders;
    }

    /**
     * Print pretty.
     *
     * @param doc
     * @param ace
     * @param acp
     */
    private void printPretty(DocumentModel doc, ACE ace, ACP acp) {
        LOG.info("ACP for " + doc.getId() + "(" + doc.getName() + ") " + (ace != null ? ace.getPermission() + ", " + ace.getUsername() : ""));
        for (ACL acl : acp.getACLs()) {
            LOG.info("ACL " + acl.getName() + ":" + acl);
        }
    }

    /**
     * Set save at end.
     * @param save
     */
    public void saveAtEnd(boolean save) {
        this.save = save;
    }
}
