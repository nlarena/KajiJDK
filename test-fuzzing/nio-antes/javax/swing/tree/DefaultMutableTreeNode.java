package javax.swing.tree;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.Vector;

/**
 * A tree node that keeps an object and a list of children.
 *
 * <h2>The node is not the datum</h2>
 *
 * <p>Each node has a {@code userObject}: the datum it represents. The node is the structure --
 * the parent, the children --, the object is what matters to the program. Separating them allows
 * building a tree over data that already exists without touching it.
 *
 * <p>{@link #toString} returns the object's {@code toString}, not the node's. It is what makes a
 * tree of strings look right without writing a renderer.
 *
 * <h2>Allowing children is not having children</h2>
 *
 * <p>{@link #setAllowsChildren} decides whether the node is a leaf even though it has no children
 * now. An empty folder allows children and that is why it carries the little expand triangle; a
 * file does not. Without that distinction, an empty folder would look like a file.
 *
 * <h2>The traversals</h2>
 *
 * <p>There are four, and they are not the same. In preorder the parent comes before its children;
 * in postorder after; by levels it is walked row by row. The one wanted almost always is
 * preorder, which is the order the rows of an expanded tree are seen in.
 */
public class DefaultMutableTreeNode implements Cloneable, MutableTreeNode, Serializable {

    /** An empty traversal, for the nodes that have no children. */
    public static final Enumeration<TreeNode> EMPTY_ENUMERATION = new EmptyEnumeration();

    /** The parent, or null if it is the root. */
    protected MutableTreeNode parent;

    /** The children; null while there is none. */
    protected Vector<MutableTreeNode> children;

    /** The datum this node represents. */
    protected transient Object userObject;

    /** Whether the node may have children; see the class note. */
    protected boolean allowsChildren;

    /** A node with no datum, which allows children. */
    public DefaultMutableTreeNode() {
        this(null);
    }

    /** A node with that datum, which allows children. */
    public DefaultMutableTreeNode(Object userObject) {
        this(userObject, true);
    }

    /** A node with that datum; {@code allowsChildren} decides whether it is a leaf. */
    public DefaultMutableTreeNode(Object userObject, boolean allowsChildren) {
        super();
        parent = null;
        this.allowsChildren = allowsChildren;
        this.userObject = userObject;
    }

    /**
     * Inserts a child at that position.
     *
     * <p>It takes it out of its previous parent first: a node cannot be in two places, and leaving
     * it would break the walk upwards.
     *
     * @throws IllegalArgumentException if the node is null or is an ancestor of this one.
     * @throws IllegalStateException if this node does not allow children.
     */
    public void insert(MutableTreeNode newChild, int childIndex) {
        if (!allowsChildren) {
            throw new IllegalStateException("node does not allow children");
        }
        if (newChild == null) {
            throw new IllegalArgumentException("new child is null");
        }
        if (isNodeAncestor(newChild)) {
            throw new IllegalArgumentException("new child is an ancestor");
        }
        MutableTreeNode oldParent = (MutableTreeNode) newChild.getParent();
        if (oldParent != null) {
            oldParent.remove(newChild);
        }
        newChild.setParent(this);
        if (children == null) {
            children = new Vector<MutableTreeNode>();
        }
        children.insertElementAt(newChild, childIndex);
    }

    /** Removes child number such and such. */
    public void remove(int childIndex) {
        MutableTreeNode child = (MutableTreeNode) getChildAt(childIndex);
        children.removeElementAt(childIndex);
        child.setParent(null);
    }

    /** Changes the parent; the parent calls it, not whoever uses the tree. */
    public void setParent(MutableTreeNode newParent) {
        parent = newParent;
    }

    public TreeNode getParent() {
        return parent;
    }

    /**
     * Child number such and such.
     *
     * @throws ArrayIndexOutOfBoundsException if it does not exist.
     */
    public TreeNode getChildAt(int index) {
        if (children == null) {
            throw new ArrayIndexOutOfBoundsException("node has no children");
        }
        return children.elementAt(index);
    }

    public int getChildCount() {
        if (children == null) {
            return 0;
        }
        return children.size();
    }

    /**
     * At what position that child is, or -1.
     *
     * @throws IllegalArgumentException if the node is null.
     */
    public int getIndex(TreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }
        if (!isNodeChild(aChild)) {
            return -1;
        }
        return children.indexOf(aChild);
    }

    public Enumeration<TreeNode> children() {
        if (children == null) {
            return EMPTY_ENUMERATION;
        }
        return new ChildrenEnumeration(children);
    }

    /** Whether the node may have children; see the class note. */
    public void setAllowsChildren(boolean allows) {
        if (allows != allowsChildren) {
            allowsChildren = allows;
            if (!allowsChildren) {
                removeAllChildren();
            }
        }
    }

    public boolean getAllowsChildren() {
        return allowsChildren;
    }

    /** The datum this node represents. */
    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }

    public Object getUserObject() {
        return userObject;
    }

    /** It takes itself out of its parent. */
    public void removeFromParent() {
        MutableTreeNode parent = (MutableTreeNode) getParent();
        if (parent != null) {
            parent.remove(this);
        }
    }

    /**
     * Removes that child.
     *
     * @throws IllegalArgumentException if it is not a child of this node.
     */
    public void remove(MutableTreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }
        if (!isNodeChild(aChild)) {
            throw new IllegalArgumentException("argument is not a child");
        }
        remove(getIndex(aChild));
    }

    public void removeAllChildren() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            remove(i);
        }
    }

    /** Adds a child at the end. */
    public void add(MutableTreeNode newChild) {
        if (newChild != null && newChild.getParent() == this) {
            insert(newChild, getChildCount() - 1);
        } else {
            insert(newChild, getChildCount());
        }
    }

    /** Whether that node is on the path to the root from this one. */
    public boolean isNodeAncestor(TreeNode anotherNode) {
        if (anotherNode == null) {
            return false;
        }
        TreeNode ancestor = this;
        do {
            if (ancestor == anotherNode) {
                return true;
            }
            ancestor = ancestor.getParent();
        } while (ancestor != null);
        return false;
    }

    /** Whether this node is on the path to the root from that one. */
    public boolean isNodeDescendant(DefaultMutableTreeNode anotherNode) {
        if (anotherNode == null) {
            return false;
        }
        return anotherNode.isNodeAncestor(this);
    }

    /** The nearest ancestor both share, or null. */
    public TreeNode getSharedAncestor(DefaultMutableTreeNode aNode) {
        if (aNode == this) {
            return this;
        }
        if (aNode == null) {
            return null;
        }
        int level1 = getLevel();
        int level2 = aNode.getLevel();
        TreeNode node1;
        TreeNode node2;
        int diff;
        if (level2 > level1) {
            diff = level2 - level1;
            node1 = aNode;
            node2 = this;
        } else {
            diff = level1 - level2;
            node1 = this;
            node2 = aNode;
        }
        // The deeper one is walked up until they are level, and then both together.
        while (diff > 0) {
            node1 = node1.getParent();
            diff--;
        }
        do {
            if (node1 == node2) {
                return node1;
            }
            node1 = node1.getParent();
            node2 = node2.getParent();
        } while (node1 != null);
        return null;
    }

    /** Whether both are in the same tree. */
    public boolean isNodeRelated(DefaultMutableTreeNode aNode) {
        return (aNode != null) && (getRoot() == aNode.getRoot());
    }

    /** How many levels there are below this node. */
    public int getDepth() {
        Object last = null;
        Enumeration<TreeNode> enum_ = breadthFirstEnumeration();
        while (enum_.hasMoreElements()) {
            last = enum_.nextElement();
        }
        if (last == null) {
            throw new Error("nodes should be null");
        }
        return ((DefaultMutableTreeNode) last).getLevel() - getLevel();
    }

    /** How many levels there are above this node. */
    public int getLevel() {
        TreeNode ancestor = this;
        int levels = 0;
        while ((ancestor = ancestor.getParent()) != null) {
            levels++;
        }
        return levels;
    }

    /** The path from the root to this node. */
    public TreeNode[] getPath() {
        return getPathToRoot(this, 0);
    }

    /**
     * It builds the path by walking up and filling the array backwards.
     *
     * <p>It walks up counting first and fills in afterwards, because the path's length is not known
     * until the root is reached.
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
            retNodes = getPathToRoot(aNode.getParent(), depth);
            retNodes[retNodes.length - depth] = aNode;
        }
        return retNodes;
    }

    /** The path nodes' data, not the nodes. */
    public Object[] getUserObjectPath() {
        TreeNode[] realPath = getPath();
        Object[] retPath = new Object[realPath.length];
        for (int counter = 0; counter < realPath.length; counter++) {
            retPath[counter] = ((DefaultMutableTreeNode) realPath[counter]).getUserObject();
        }
        return retPath;
    }

    public TreeNode getRoot() {
        TreeNode ancestor = this;
        TreeNode previous;
        do {
            previous = ancestor;
            ancestor = ancestor.getParent();
        } while (ancestor != null);
        return previous;
    }

    public boolean isRoot() {
        return getParent() == null;
    }

    /**
     * The node that follows in preorder, or null if it is the last.
     *
     * <p>It walks the whole tree, not only the siblings: after the last child comes the parent's
     * sibling.
     */
    public DefaultMutableTreeNode getNextNode() {
        if (getChildCount() == 0) {
            DefaultMutableTreeNode nextSibling = getNextSibling();
            if (nextSibling == null) {
                DefaultMutableTreeNode aNode = (DefaultMutableTreeNode) getParent();
                do {
                    if (aNode == null) {
                        return null;
                    }
                    nextSibling = aNode.getNextSibling();
                    if (nextSibling != null) {
                        return nextSibling;
                    }
                    aNode = (DefaultMutableTreeNode) aNode.getParent();
                } while (true);
            }
            return nextSibling;
        }
        return (DefaultMutableTreeNode) getChildAt(0);
    }

    /** The previous one in preorder, or null. */
    public DefaultMutableTreeNode getPreviousNode() {
        DefaultMutableTreeNode previousSibling;
        DefaultMutableTreeNode myParent = (DefaultMutableTreeNode) getParent();
        if (myParent == null) {
            return null;
        }
        previousSibling = getPreviousSibling();
        if (previousSibling != null) {
            if (previousSibling.getChildCount() == 0) {
                return previousSibling;
            }
            return previousSibling.getLastLeaf();
        }
        return myParent;
    }

    public Enumeration<TreeNode> preorderEnumeration() {
        return new PreorderEnumeration(this);
    }

    public Enumeration<TreeNode> postorderEnumeration() {
        return new PostorderEnumeration(this);
    }

    public Enumeration<TreeNode> breadthFirstEnumeration() {
        return new BreadthFirstEnumeration(this);
    }

    /** The same as {@link #preorderEnumeration}, with the usual name. */
    public Enumeration<TreeNode> depthFirstEnumeration() {
        return postorderEnumeration();
    }

    /**
     * Walks the path from the root to this node.
     *
     * @throws IllegalArgumentException if the given node is not an ancestor of this one.
     */
    public Enumeration<TreeNode> pathFromAncestorEnumeration(TreeNode ancestor) {
        return new PathEnumeration(this, ancestor);
    }

    public boolean isNodeChild(TreeNode aNode) {
        if (aNode == null) {
            return false;
        }
        if (getChildCount() == 0) {
            return false;
        }
        return (aNode.getParent() == this);
    }

    /**
     * The first child.
     *
     * @throws NoSuchElementException if it has no children.
     */
    public TreeNode getFirstChild() {
        if (getChildCount() == 0) {
            throw new NoSuchElementException("node has no children");
        }
        return getChildAt(0);
    }

    /**
     * The last child.
     *
     * @throws NoSuchElementException if it has no children.
     */
    public TreeNode getLastChild() {
        if (getChildCount() == 0) {
            throw new NoSuchElementException("node has no children");
        }
        return getChildAt(getChildCount() - 1);
    }

    /**
     * The child that follows that one.
     *
     * @throws IllegalArgumentException if it is not a child of this node.
     */
    public TreeNode getChildAfter(TreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }
        int index = getIndex(aChild);
        if (index == -1) {
            throw new IllegalArgumentException("node is not a child");
        }
        if (index < getChildCount() - 1) {
            return getChildAt(index + 1);
        }
        return null;
    }

    /**
     * The child before that one.
     *
     * @throws IllegalArgumentException if it is not a child of this node.
     */
    public TreeNode getChildBefore(TreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }
        int index = getIndex(aChild);
        if (index == -1) {
            throw new IllegalArgumentException("argument is not a child");
        }
        if (index > 0) {
            return getChildAt(index - 1);
        }
        return null;
    }

    public boolean isNodeSibling(TreeNode anotherNode) {
        if (anotherNode == null) {
            return false;
        }
        if (anotherNode == this) {
            return true;
        }
        TreeNode myParent = getParent();
        return (myParent != null && myParent == anotherNode.getParent());
    }

    public int getSiblingCount() {
        TreeNode myParent = getParent();
        if (myParent == null) {
            return 1;
        }
        return myParent.getChildCount();
    }

    public DefaultMutableTreeNode getNextSibling() {
        DefaultMutableTreeNode retval;
        DefaultMutableTreeNode myParent = (DefaultMutableTreeNode) getParent();
        if (myParent == null) {
            retval = null;
        } else {
            retval = (DefaultMutableTreeNode) myParent.getChildAfter(this);
        }
        return retval;
    }

    public DefaultMutableTreeNode getPreviousSibling() {
        DefaultMutableTreeNode retval;
        DefaultMutableTreeNode myParent = (DefaultMutableTreeNode) getParent();
        if (myParent == null) {
            retval = null;
        } else {
            retval = (DefaultMutableTreeNode) myParent.getChildBefore(this);
        }
        return retval;
    }

    /** Whether it has no children; different from not allowing them. See the class note. */
    public boolean isLeaf() {
        return (getChildCount() == 0);
    }

    /** The first leaf going always down the first child. */
    public DefaultMutableTreeNode getFirstLeaf() {
        DefaultMutableTreeNode node = this;
        while (!node.isLeaf()) {
            node = (DefaultMutableTreeNode) node.getFirstChild();
        }
        return node;
    }

    public DefaultMutableTreeNode getLastLeaf() {
        DefaultMutableTreeNode node = this;
        while (!node.isLeaf()) {
            node = (DefaultMutableTreeNode) node.getLastChild();
        }
        return node;
    }

    /** The leaf that follows in the whole tree, not only under this node. */
    public DefaultMutableTreeNode getNextLeaf() {
        DefaultMutableTreeNode nextSibling;
        DefaultMutableTreeNode myParent = (DefaultMutableTreeNode) getParent();
        if (myParent == null) {
            return null;
        }
        nextSibling = getNextSibling();
        if (nextSibling != null) {
            return nextSibling.getFirstLeaf();
        }
        return myParent.getNextLeaf();
    }

    public DefaultMutableTreeNode getPreviousLeaf() {
        DefaultMutableTreeNode previousSibling;
        DefaultMutableTreeNode myParent = (DefaultMutableTreeNode) getParent();
        if (myParent == null) {
            return null;
        }
        previousSibling = getPreviousSibling();
        if (previousSibling != null) {
            return previousSibling.getLastLeaf();
        }
        return myParent.getPreviousLeaf();
    }

    /** How many leaves hang from this node. */
    public int getLeafCount() {
        int count = 0;
        Enumeration<TreeNode> enum_ = breadthFirstEnumeration();
        while (enum_.hasMoreElements()) {
            TreeNode node = enum_.nextElement();
            if (node.isLeaf()) {
                count++;
            }
        }
        return count;
    }

    /** The datum's {@code toString}, not the node's; see the class note. */
    public String toString() {
        if (userObject == null) {
            return null;
        }
        return userObject.toString();
    }

    /**
     * A copy of the node, with no parent and no children.
     *
     * <p>A shallow copy on purpose: copying the subtree would be expensive and is almost never what
     * is wanted. Whoever wants the subtree walks it.
     */
    public Object clone() {
        DefaultMutableTreeNode newNode;
        try {
            newNode = (DefaultMutableTreeNode) super.clone();
            newNode.children = null;
            newNode.parent = null;
        } catch (CloneNotSupportedException e) {
            throw new Error(e.toString());
        }
        return newNode;
    }

    /** The empty traversal; see {@link #EMPTY_ENUMERATION}. */
    static final class EmptyEnumeration implements Enumeration<TreeNode> {

        public boolean hasMoreElements() {
            return false;
        }

        public TreeNode nextElement() {
            throw new NoSuchElementException("No more elements");
        }
    }

    /** A node's children, in order. */
    static final class ChildrenEnumeration implements Enumeration<TreeNode> {

        private final Vector<MutableTreeNode> children;
        private int i = 0;

        ChildrenEnumeration(Vector<MutableTreeNode> children) {
            this.children = children;
        }

        public boolean hasMoreElements() {
            return i < children.size();
        }

        public TreeNode nextElement() {
            if (!hasMoreElements()) {
                throw new NoSuchElementException("No more elements");
            }
            TreeNode n = children.elementAt(i);
            i++;
            return n;
        }
    }

    /**
     * Preorder traversal: the parent before its children.
     *
     * <p>It carries a stack of pending traversals instead of recursion, so that a very deep tree
     * does not overflow.
     */
    static final class PreorderEnumeration implements Enumeration<TreeNode> {

        private final Stack<Enumeration<TreeNode>> stack = new Stack<Enumeration<TreeNode>>();

        PreorderEnumeration(TreeNode root) {
            Vector<TreeNode> v = new Vector<TreeNode>(1);
            v.addElement(root);
            stack.push(v.elements());
        }

        public boolean hasMoreElements() {
            return (!stack.empty() && stack.peek().hasMoreElements());
        }

        public TreeNode nextElement() {
            Enumeration<TreeNode> above = stack.peek();
            TreeNode node = above.nextElement();
            if (!above.hasMoreElements()) {
                stack.pop();
            }
            Enumeration<? extends TreeNode> children = node.children();
            if (children.hasMoreElements()) {
                stack.push((Enumeration<TreeNode>) children);
            }
            return node;
        }
    }

    /** Postorder traversal: the children before the parent. */
    static final class PostorderEnumeration implements Enumeration<TreeNode> {

        private TreeNode root;
        private Enumeration<? extends TreeNode> children;
        private Enumeration<TreeNode> subtree;

        PostorderEnumeration(TreeNode root) {
            this.root = root;
            children = root.children();
            subtree = DefaultMutableTreeNode.EMPTY_ENUMERATION;
        }

        public boolean hasMoreElements() {
            return root != null;
        }

        public TreeNode nextElement() {
            TreeNode retval;
            if (subtree.hasMoreElements()) {
                retval = subtree.nextElement();
            } else if (children.hasMoreElements()) {
                subtree = new PostorderEnumeration(children.nextElement());
                retval = subtree.nextElement();
            } else {
                retval = root;
                root = null;
            }
            return retval;
        }
    }

    /** Traversal by levels: row by row. */
    static final class BreadthFirstEnumeration implements Enumeration<TreeNode> {

        private final java.util.LinkedList<Enumeration<? extends TreeNode>> queue =
                new java.util.LinkedList<Enumeration<? extends TreeNode>>();

        BreadthFirstEnumeration(TreeNode root) {
            Vector<TreeNode> v = new Vector<TreeNode>(1);
            v.addElement(root);
            queue.addLast(v.elements());
        }

        public boolean hasMoreElements() {
            return (!queue.isEmpty() && queue.getFirst().hasMoreElements());
        }

        public TreeNode nextElement() {
            Enumeration<? extends TreeNode> first = queue.getFirst();
            TreeNode node = first.nextElement();
            if (!first.hasMoreElements()) {
                queue.removeFirst();
            }
            Enumeration<? extends TreeNode> children = node.children();
            if (children.hasMoreElements()) {
                queue.addLast(children);
            }
            return node;
        }
    }

    /** Walks the path from an ancestor down to a node. */
    static final class PathEnumeration implements Enumeration<TreeNode> {

        private final Stack<TreeNode> stack = new Stack<TreeNode>();

        PathEnumeration(TreeNode node, TreeNode ancestor) {
            if (node == null || ancestor == null) {
                throw new IllegalArgumentException("argument is null");
            }
            TreeNode n = node;
            while (n != null && n != ancestor) {
                stack.push(n);
                n = n.getParent();
            }
            if (n != ancestor) {
                throw new IllegalArgumentException("node is not an ancestor");
            }
            stack.push(ancestor);
        }

        public boolean hasMoreElements() {
            return !stack.isEmpty();
        }

        public TreeNode nextElement() {
            if (stack.isEmpty()) {
                throw new NoSuchElementException("No more elements");
            }
            return stack.pop();
        }
    }
}
