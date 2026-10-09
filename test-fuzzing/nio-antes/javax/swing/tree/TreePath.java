package javax.swing.tree;

/**
 * The path from a tree's root to a node.
 *
 * <h2>Why a path and not the node</h2>
 *
 * <p>Because one same object may hang in two places of the tree. If the selection were "the
 * node", there would be no way of saying <em>which</em> of its appearances. The path
 * disambiguates, and that is why the whole of Swing's tree model talks in paths and not in
 * nodes.
 *
 * <h2>Immutable, and sharing the tail</h2>
 *
 * <p>A {@code TreePath} never changes. {@link #pathByAddingChild} returns a new one that
 * <strong>keeps the previous one as its parent</strong> instead of copying the array: a tree of a
 * thousand nodes of depth ten does not allocate ten thousand elements, it shares the prefixes. It
 * is the same idea as a persistent linked list.
 *
 * <p>Hence {@link #getPath} has to rebuild the array by walking the chain: the complete array does
 * not exist until somebody asks for it.
 */
public class TreePath implements java.io.Serializable {

    private static final long serialVersionUID = 4380089275673032332L;

    /** The last component: the node this path points at. */
    private Object lastPathComponent;

    /** The path up to the parent, or {@code null} if this is the root. */
    private TreePath parentPath;

    /**
     * A path with those components, from the root to the node.
     *
     * @throws IllegalArgumentException if the array is {@code null} or empty
     */
    public TreePath(Object[] path) {
        if (path == null || path.length == 0) {
            throw new IllegalArgumentException("The path cannot be null or empty");
        }
        this.lastPathComponent = path[path.length - 1];
        if (path.length > 1) {
            this.parentPath = new TreePath(path, path.length - 1);
        }
    }

    /**
     * A path of a single component, that is the root.
     *
     * @throws IllegalArgumentException if it is {@code null}
     */
    public TreePath(Object singlePath) {
        if (singlePath == null) {
            throw new IllegalArgumentException("The component cannot be null");
        }
        this.lastPathComponent = singlePath;
        this.parentPath = null;
    }

    /** {@code parent}'s path plus a child. It is what shares the prefix. */
    protected TreePath(TreePath parent, Object lastElement) {
        if (lastElement == null) {
            throw new IllegalArgumentException("The component cannot be null");
        }
        this.parentPath = parent;
        this.lastPathComponent = lastElement;
    }

    /** The first {@code length} components of {@code path}. */
    protected TreePath(Object[] path, int length) {
        this.lastPathComponent = path[length - 1];
        if (length > 1) {
            this.parentPath = new TreePath(path, length - 1);
        }
    }

    /** With no components. For the subclasses that keep them some other way. */
    protected TreePath() {
    }

    /** The path as an array, from the root to the node. */
    public Object[] getPath() {
        int n = getPathCount();
        Object[] result = new Object[n];
        TreePath current = this;
        for (int i = n - 1; i >= 0; i--) {
            result[i] = current.getLastPathComponent();
            current = current.getParentPath();
        }
        return result;
    }

    /** The node it points at. */
    public Object getLastPathComponent() {
        return this.lastPathComponent;
    }

    /** How many components it has, counting the root. */
    public int getPathCount() {
        int n = 0;
        TreePath current = this;
        while (current != null) {
            n = n + 1;
            current = current.getParentPath();
        }
        return n;
    }

    /**
     * Component number {@code element}, counting from the root.
     *
     * @throws IllegalArgumentException if the index is out of range
     */
    public Object getPathComponent(int element) {
        int n = getPathCount();
        if (element < 0 || element >= n) {
            throw new IllegalArgumentException("Index out of range: " + String.valueOf(element));
        }
        TreePath current = this;
        for (int i = n - 1; i != element; i--) {
            current = current.getParentPath();
        }
        return current.getLastPathComponent();
    }

    /**
     * Equal if they have the same components in the same order.
     *
     * <p>It compares with {@code equals} and not by identity, so two paths built separately over
     * the same nodes are equal -- which is what is needed for a rebuilt selection to go on being
     * the same one.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof TreePath) {
            TreePath other = (TreePath) o;
            if (getPathCount() != other.getPathCount()) {
                return false;
            }
            TreePath a = this;
            TreePath b = other;
            while (a != null) {
                if (!a.getLastPathComponent().equals(b.getLastPathComponent())) {
                    return false;
                }
                a = a.getParentPath();
                b = b.getParentPath();
            }
            return true;
        }
        return false;
    }

    /**
     * The last component's hash.
     *
     * <p>It is enough, and it is what the JDK does: two equal paths end at the same node, so the
     * property a hash needs holds. Walking the whole chain would only make the computation more
     * expensive without separating any better.
     */
    public int hashCode() {
        return this.lastPathComponent.hashCode();
    }

    /** Whether {@code aTreePath} hangs from this path, or is this same one. */
    public boolean isDescendant(TreePath aTreePath) {
        if (aTreePath == this) {
            return true;
        }
        if (aTreePath == null) {
            return false;
        }
        int myLength = getPathCount();
        int otherLength = aTreePath.getPathCount();
        if (otherLength < myLength) {
            return false;
        }
        // The candidate is walked up until it is at the same height, and only then are they
        // compared.
        TreePath candidate = aTreePath;
        while (otherLength > myLength) {
            candidate = candidate.getParentPath();
            otherLength = otherLength - 1;
        }
        return equals(candidate);
    }

    /** This path plus a child. It shares the prefix; see the class note. */
    public TreePath pathByAddingChild(Object child) {
        if (child == null) {
            throw new NullPointerException("The child cannot be null");
        }
        return new TreePath(this, child);
    }

    /** The path up to the parent, or {@code null} if this is the root. */
    public TreePath getParentPath() {
        return this.parentPath;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        int n = getPathCount();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(getPathComponent(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
