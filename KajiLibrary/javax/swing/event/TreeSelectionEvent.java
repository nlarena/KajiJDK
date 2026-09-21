package javax.swing.event;

import java.util.EventObject;

import javax.swing.tree.TreePath;

/**
 * The tree's selection changed.
 *
 * <h2>It brings the DIFFERENCE, not the selection</h2>
 *
 * <p>{@link #getPaths} returns the paths that <em>changed state</em>, and the parallel array
 * {@code areNew} says whether each one came in or went out. It is not the current selection: for
 * that the tree has to be asked.
 *
 * <p>Bringing the difference is what makes selecting a thousand rows cheap -- the event describes
 * what moved, not what was left.
 *
 * <p>The <em>lead</em> path is the last one the user touched, and it comes with its previous
 * value: it moves the keyboard focus, and whoever draws needs to repaint both.
 */
public class TreeSelectionEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** The paths that changed state. */
    protected TreePath[] paths;

    /** For each one, whether it came in ({@code true}) or went out. */
    protected boolean[] areNew;

    /** The previous lead. */
    protected TreePath oldLeadSelectionPath;

    /** The new lead. */
    protected TreePath newLeadSelectionPath;

    /** With several paths. */
    public TreeSelectionEvent(Object source, TreePath[] paths, boolean[] areNew,
            TreePath oldLeadSelectionPath, TreePath newLeadSelectionPath) {
        super(source);
        this.paths = paths;
        this.areNew = areNew;
        this.oldLeadSelectionPath = oldLeadSelectionPath;
        this.newLeadSelectionPath = newLeadSelectionPath;
    }

    /** With a single one. */
    public TreeSelectionEvent(Object source, TreePath path, boolean isNew,
            TreePath oldLeadSelectionPath, TreePath newLeadSelectionPath) {
        super(source);
        this.paths = new TreePath[1];
        this.paths[0] = path;
        this.areNew = new boolean[1];
        this.areNew[0] = isNew;
        this.oldLeadSelectionPath = oldLeadSelectionPath;
        this.newLeadSelectionPath = newLeadSelectionPath;
    }

    /** The paths that changed, in a new array. */
    public TreePath[] getPaths() {
        int n = this.paths.length;
        TreePath[] copy = new TreePath[n];
        for (int i = 0; i < n; i++) {
            copy[i] = this.paths[i];
        }
        return copy;
    }

    /** The first of the paths that changed. */
    public TreePath getPath() {
        return this.paths[0];
    }

    /** Whether {@link #getPath} came into the selection. */
    public boolean isAddedPath() {
        return this.areNew[0];
    }

    /** Whether that path came into the selection. */
    public boolean isAddedPath(TreePath path) {
        for (int i = this.paths.length - 1; i >= 0; i--) {
            if (this.paths[i].equals(path)) {
                return this.areNew[i];
            }
        }
        throw new IllegalArgumentException("that path is not in the event");
    }

    /** Whether path number {@code index} came into the selection. */
    public boolean isAddedPath(int index) {
        if (this.paths == null || index < 0 || index >= this.paths.length) {
            throw new IllegalArgumentException("index out of range");
        }
        return this.areNew[index];
    }

    /** The previous lead. */
    public TreePath getOldLeadSelectionPath() {
        return this.oldLeadSelectionPath;
    }

    /** The new lead. */
    public TreePath getNewLeadSelectionPath() {
        return this.newLeadSelectionPath;
    }

    /** A copy with another source, for forwarding it. */
    public Object cloneWithSource(Object newSource) {
        return new TreeSelectionEvent(newSource, this.paths, this.areNew,
                this.oldLeadSelectionPath, this.newLeadSelectionPath);
    }
}
