package javax.swing.tree;

import javax.swing.event.TreeModelListener;

/**
 * A tree's data.
 *
 * <h2>The nodes are {@code Object}s</h2>
 *
 * <p>The model asks for no interface for its nodes: they are any objects at all, and the model is
 * the one that knows how to ask them for children. It is what allows showing a tree of files, one
 * of XML or one from a database without wrapping every node in a Swing class.
 *
 * <p>{@link DefaultTreeModel} is the easy case, where the nodes are {@link TreeNode}s.
 *
 * <h2>Why reporting is needed</h2>
 *
 * <p>A tree shows only what is expanded and remembers which row each node is. A change that is
 * not reported leaves that bookkeeping stale, and from then on the tree shows one node and
 * chooses another.
 */
public interface TreeModel {

    /** The root; null means an empty tree. */
    Object getRoot();

    /** Child number such and such of that node. */
    Object getChild(Object parent, int index);

    int getChildCount(Object parent);

    /**
     * Whether that node cannot have children.
     *
     * <p>It is different from having none: an empty folder is not a leaf, and that is why it is
     * drawn with the little expand triangle.
     */
    boolean isLeaf(Object node);

    /** The user edited the node at that path and it was left with that value. */
    void valueForPathChanged(TreePath path, Object newValue);

    /** At what position that child is, or -1. */
    int getIndexOfChild(Object parent, Object child);

    void addTreeModelListener(TreeModelListener l);

    void removeTreeModelListener(TreeModelListener l);
}
