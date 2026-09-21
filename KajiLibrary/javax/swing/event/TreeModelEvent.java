package javax.swing.event;

import java.util.EventObject;

import javax.swing.tree.TreePath;

/**
 * The tree changed.
 *
 * <h2>The path is to the PARENT, not to what changed</h2>
 *
 * <p>It is this class's trap. {@link #getTreePath} returns the path to the node <em>whose
 * children</em> changed, and {@link #getChildIndices} says which ones. Reading it as "the node
 * that changed" gives the wrong result in every case but one.
 *
 * <p>The exception is {@code treeStructureChanged}, where the path points at the root of the
 * subtree that was redone whole and the indices are {@code null}: there are no children to
 * enumerate because they all changed.
 */
public class TreeModelEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** The path to the parent. */
    protected TreePath path;

    /** Which children changed, in increasing order. */
    protected int[] childIndices;

    /** The children that changed. */
    protected Object[] children;

    /** With the path as an array of nodes. */
    public TreeModelEvent(Object source, Object[] path, int[] childIndices, Object[] children) {
        this(source, path == null ? null : new TreePath(path), childIndices, children);
    }

    /** With the path as a {@link TreePath}. */
    public TreeModelEvent(Object source, TreePath path, int[] childIndices, Object[] children) {
        super(source);
        this.path = path;
        this.childIndices = childIndices;
        this.children = children;
    }

    /** The structure under that path changed, given as an array. */
    public TreeModelEvent(Object source, Object[] path) {
        this(source, path == null ? null : new TreePath(path));
    }

    /** The structure under that path changed. */
    public TreeModelEvent(Object source, TreePath path) {
        super(source);
        this.path = path;
        this.childIndices = new int[0];
    }

    /** The path to the parent; see the class note. */
    public TreePath getTreePath() {
        return this.path;
    }

    /** The same path, as an array of nodes. */
    public Object[] getPath() {
        if (this.path != null) {
            return this.path.getPath();
        }
        return null;
    }

    /** The children that changed, in a new array. */
    public Object[] getChildren() {
        if (this.children == null) {
            return null;
        }
        Object[] copy = new Object[this.children.length];
        for (int i = 0; i < this.children.length; i++) {
            copy[i] = this.children[i];
        }
        return copy;
    }

    /** The indices of the children that changed, in a new array. */
    public int[] getChildIndices() {
        if (this.childIndices == null) {
            return null;
        }
        int[] copy = new int[this.childIndices.length];
        for (int i = 0; i < this.childIndices.length; i++) {
            copy[i] = this.childIndices[i];
        }
        return copy;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName() + " " + String.valueOf(hashCode()));
        if (this.path != null) {
            sb.append(" path " + this.path);
        }
        if (this.childIndices != null) {
            sb.append(" indices [");
            for (int i = 0; i < this.childIndices.length; i++) {
                sb.append(" " + String.valueOf(this.childIndices[i]));
            }
            sb.append(" ]");
        }
        return sb.toString();
    }
}
