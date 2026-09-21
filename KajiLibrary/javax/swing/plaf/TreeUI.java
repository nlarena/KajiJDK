package javax.swing.plaf;

import java.awt.Rectangle;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * A {@link JTree}'s look and feel.
 *
 * <h2>Geometry and editing</h2>
 *
 * <p>The first five methods translate between paths, rows and points. The tree cannot do it:
 * which row a path falls on depends on whether it is expanded and on how much each node
 * measures, and both things are decided by the look and feel.
 *
 * <p>The other five are about in-place editing. That also belongs to the look and feel because
 * the editor is a component the look and feel adds to and removes from the tree.
 */
public abstract class TreeUI extends ComponentUI {

    protected TreeUI() {
    }

    /** That path's rectangle, or null if it is not seen. */
    public abstract Rectangle getPathBounds(JTree tree, TreePath path);

    public abstract TreePath getPathForRow(JTree tree, int row);

    /** That path's row, or -1 if it is not seen. */
    public abstract int getRowForPath(JTree tree, TreePath path);

    /** How many rows are seen. */
    public abstract int getRowCount(JTree tree);

    /** The path nearest that point, even if the point falls on none. */
    public abstract TreePath getClosestPathForLocation(JTree tree, int x, int y);

    public abstract boolean isEditing(JTree tree);

    /** Ends the editing, saving; it returns whether there was one. */
    public abstract boolean stopEditing(JTree tree);

    public abstract void cancelEditing(JTree tree);

    public abstract void startEditingAtPath(JTree tree, TreePath path);

    public abstract TreePath getEditingPath(JTree tree);
}
