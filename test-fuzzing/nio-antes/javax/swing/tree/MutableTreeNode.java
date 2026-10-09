package javax.swing.tree;

/**
 * A tree node to which children can be added and removed.
 *
 * <p>{@link TreeNode} only allows looking. This one adds what is needed to build the tree and to
 * move it, and it is what {@link DefaultTreeModel} expects when it is asked to change something.
 *
 * <p>{@link #removeFromParent} and {@link #setParent} go together: moving a node is taking it out
 * of one parent and putting it in another, and doing it in a single step would leave the tree
 * with a node in two places.
 */
public interface MutableTreeNode extends TreeNode {

    /** Inserts that child at that position. */
    void insert(MutableTreeNode child, int index);

    void remove(int index);

    void remove(MutableTreeNode node);

    /** The object this node represents. */
    void setUserObject(Object object);

    void removeFromParent();

    /** The parent calls it; see the interface note. */
    void setParent(MutableTreeNode newParent);
}
