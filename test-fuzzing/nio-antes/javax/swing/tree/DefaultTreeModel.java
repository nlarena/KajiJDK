package javax.swing.tree;

import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

/**
 * A tree model over {@link TreeNode} nodes.
 *
 * <h2>Two ways of saying that something is a leaf</h2>
 *
 * <p>{@link #setAsksAllowsChildren} chooses between asking whether the node <em>allows</em>
 * children or whether it <em>has</em> them. The difference shows in an empty folder: asking
 * about allowing, it is drawn with the little expand triangle; asking about having, it looks
 * like a file.
 *
 * <p>By default it asks whether it has them, which is what a data tree with no empty containers
 * wants. A file tree wants the other.
 *
 * <h2>Changing the tree is not enough</h2>
 *
 * <p>A {@link DefaultMutableTreeNode} can be changed directly, but then the model does not find
 * out and does not report. The methods {@code insertNodeInto}, {@code removeNodeFromParent} and
 * {@code nodeChanged} do both things: they change and they report.
 *
 * <p>The {@code nodesWere...} ones are for the reverse case: the tree already changed from
 * outside and only the report is missing. They serve when the change was large and it is better
 * to do it in one go and report once.
 */
public class DefaultTreeModel implements Serializable, TreeModel {

    /** The tree's root. */
    protected TreeNode root;

    /** Those who listen. */
    protected EventListenerList listenerList = new EventListenerList();

    /** Whether being a leaf is decided by allowing children; see the class note. */
    protected boolean asksAllowsChildren;

    /** A model over that tree, which decides leaf by having children. */
    public DefaultTreeModel(TreeNode root) {
        this(root, false);
    }

    /** A model over that tree, choosing how being a leaf is decided. */
    public DefaultTreeModel(TreeNode root, boolean asksAllowsChildren) {
        this.root = root;
        this.asksAllowsChildren = asksAllowsChildren;
    }

    public void setAsksAllowsChildren(boolean newValue) {
        asksAllowsChildren = newValue;
    }

    public boolean asksAllowsChildren() {
        return asksAllowsChildren;
    }

    /** Changes the root; the whole tree is rebuilt. */
    public void setRoot(TreeNode root) {
        Object oldRoot = this.root;
        this.root = root;
        if (root == null && oldRoot != null) {
            fireTreeStructureChanged(this, null);
        } else {
            nodeStructureChanged(root);
        }
    }

    public Object getRoot() {
        return root;
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null) {
            return -1;
        }
        return ((TreeNode) parent).getIndex((TreeNode) child);
    }

    public Object getChild(Object parent, int index) {
        return ((TreeNode) parent).getChildAt(index);
    }

    public int getChildCount(Object parent) {
        return ((TreeNode) parent).getChildCount();
    }

    /** Whether it is a leaf; the rule is chosen by {@link #setAsksAllowsChildren}. */
    public boolean isLeaf(Object node) {
        if (asksAllowsChildren) {
            return !((TreeNode) node).getAllowsChildren();
        }
        return ((TreeNode) node).isLeaf();
    }

    /** Reports that the whole tree changed. */
    public void reload() {
        reload(root);
    }

    /**
     * The user edited a node.
     *
     * <p>It stores the value in the node and reports. That the model does it and not the editor is
     * what allows a model over somebody else's data to translate before storing.
     */
    public void valueForPathChanged(TreePath path, Object newValue) {
        MutableTreeNode aNode = (MutableTreeNode) path.getLastPathComponent();
        aNode.setUserObject(newValue);
        nodeChanged(aNode);
    }

    /** Inserts a node and reports. */
    public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        parent.insert(newChild, index);
        int[] newIndexs = new int[1];
        newIndexs[0] = index;
        nodesWereInserted(parent, newIndexs);
    }

    /**
     * Removes a node and reports.
     *
     * @throws IllegalArgumentException if the node has no parent.
     */
    public void removeNodeFromParent(MutableTreeNode node) {
        MutableTreeNode parent = (MutableTreeNode) node.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("node does not have a parent.");
        }
        int[] childIndex = new int[1];
        Object[] removedArray = new Object[1];
        childIndex[0] = parent.getIndex(node);
        parent.remove(childIndex[0]);
        removedArray[0] = node;
        nodesWereRemoved(parent, childIndex, removedArray);
    }

    /** Reports that what that node shows changed, not its structure. */
    public void nodeChanged(TreeNode node) {
        if (listenerList != null && node != null) {
            TreeNode parent = node.getParent();
            if (parent != null) {
                int anIndex = parent.getIndex(node);
                if (anIndex != -1) {
                    int[] cIndexs = new int[1];
                    cIndexs[0] = anIndex;
                    nodesChanged(parent, cIndexs);
                }
            } else if (node == getRoot()) {
                nodesChanged(node, null);
            }
        }
    }

    /**
     * Reports that that node's subtree changed entirely.
     *
     * <p>It is the most expensive notice: the view throws away everything it knew about that
     * subtree and builds it again, including that it was expanded. It is worth it only when the
     * change is large.
     */
    public void reload(TreeNode node) {
        if (node != null) {
            fireTreeStructureChanged(this, getPathToRoot(node), null, null);
        }
    }

    /** Reports that those children were inserted, and are already in the tree. */
    public void nodesWereInserted(TreeNode node, int[] childIndices) {
        if (listenerList != null && node != null && childIndices != null
                && childIndices.length > 0) {
            int cCount = childIndices.length;
            Object[] newChildren = new Object[cCount];
            for (int counter = 0; counter < cCount; counter++) {
                newChildren[counter] = node.getChildAt(childIndices[counter]);
            }
            fireTreeNodesInserted(this, getPathToRoot(node), childIndices, newChildren);
        }
    }

    /**
     * Reports that those children were removed.
     *
     * <p>The removed nodes have to be passed because they are no longer in the tree: whoever
     * listens could not get them from anywhere else, and needs them to clean up whatever they had
     * kept about them.
     */
    public void nodesWereRemoved(TreeNode node, int[] childIndices, Object[] removedChildren) {
        if (node != null && childIndices != null) {
            fireTreeNodesRemoved(this, getPathToRoot(node), childIndices, removedChildren);
        }
    }

    /** Reports that those children changed what they show. */
    public void nodesChanged(TreeNode node, int[] childIndices) {
        if (node != null) {
            if (childIndices != null) {
                int cCount = childIndices.length;
                if (cCount > 0) {
                    Object[] cChildren = new Object[cCount];
                    for (int counter = 0; counter < cCount; counter++) {
                        cChildren[counter] = node.getChildAt(childIndices[counter]);
                    }
                    fireTreeNodesChanged(this, getPathToRoot(node), childIndices, cChildren);
                }
            } else if (node == getRoot()) {
                fireTreeNodesChanged(this, getPathToRoot(node), null, null);
            }
        }
    }

    public void nodeStructureChanged(TreeNode node) {
        if (node != null) {
            fireTreeStructureChanged(this, getPathToRoot(node), null, null);
        }
    }

    /** The path from the root to that node. */
    public TreeNode[] getPathToRoot(TreeNode aNode) {
        return getPathToRoot(aNode, 0);
    }

    /**
     * It builds the path walking up; see {@link DefaultMutableTreeNode#getPathToRoot}.
     *
     * <p>If the node does not reach this model's root, the path comes out all the same but
     * incomplete: the model cannot know whether the tree was rebuilt underneath it.
     */
    protected TreeNode[] getPathToRoot(TreeNode aNode, int depth) {
        TreeNode[] retNodes;
        if (aNode == null) {
            if (depth == 0) {
                return null;
            }
            retNodes = new TreeNode[depth];
        } else {
            depth++;
            if (aNode == root) {
                retNodes = new TreeNode[depth];
            } else {
                retNodes = getPathToRoot(aNode.getParent(), depth);
            }
            retNodes[retNodes.length - depth] = aNode;
        }
        return retNodes;
    }

    public void addTreeModelListener(TreeModelListener l) {
        listenerList.add(TreeModelListener.class, l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listenerList.remove(TreeModelListener.class, l);
    }

    public TreeModelListener[] getTreeModelListeners() {
        return listenerList.getListeners(TreeModelListener.class);
    }

    protected void fireTreeNodesChanged(Object source, Object[] path, int[] childIndices,
            Object[] children) {
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeModelListener.class) {
                if (e == null) {
                    e = new TreeModelEvent(source, path, childIndices, children);
                }
                ((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
            }
        }
    }

    protected void fireTreeNodesInserted(Object source, Object[] path, int[] childIndices,
            Object[] children) {
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeModelListener.class) {
                if (e == null) {
                    e = new TreeModelEvent(source, path, childIndices, children);
                }
                ((TreeModelListener) listeners[i + 1]).treeNodesInserted(e);
            }
        }
    }

    protected void fireTreeNodesRemoved(Object source, Object[] path, int[] childIndices,
            Object[] children) {
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeModelListener.class) {
                if (e == null) {
                    e = new TreeModelEvent(source, path, childIndices, children);
                }
                ((TreeModelListener) listeners[i + 1]).treeNodesRemoved(e);
            }
        }
    }

    protected void fireTreeStructureChanged(Object source, Object[] path, int[] childIndices,
            Object[] children) {
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeModelListener.class) {
                if (e == null) {
                    e = new TreeModelEvent(source, path, childIndices, children);
                }
                ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
            }
        }
    }

    /** The structure notice with a path already built. */
    private void fireTreeStructureChanged(Object source, TreePath path) {
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeModelListener.class) {
                if (e == null) {
                    e = new TreeModelEvent(source, path);
                }
                ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }
}
