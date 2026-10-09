package javax.swing.tree;

import java.util.Enumeration;

/**
 * A tree node seen from outside: children, parent and little more.
 *
 * <p>It is read-only on purpose. A {@code JTree} does not build the tree, it shows it; whoever
 * builds it uses {@code MutableTreeNode} or a model of their own. That a text document's
 * structure implements this interface is what allows looking at it with a tree without converting
 * anything.
 *
 * <p>{@link #getAllowsChildren} and {@link #isLeaf} are not the same thing: an empty folder
 * <em>admits</em> children and has none, and that difference is what decides whether the little
 * triangle to expand it is drawn.
 */
public interface TreeNode {

    TreeNode getChildAt(int childIndex);

    int getChildCount();

    /** The parent, or {@code null} if it is the root. */
    TreeNode getParent();

    /** That node's position among the children, or {@code -1} if it is not a child of this one. */
    int getIndex(TreeNode node);

    /** Whether it admits children; see the interface note. */
    boolean getAllowsChildren();

    boolean isLeaf();

    Enumeration<? extends TreeNode> children();
}
