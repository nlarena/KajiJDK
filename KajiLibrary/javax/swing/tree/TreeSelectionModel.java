package javax.swing.tree;

import java.beans.PropertyChangeListener;

import javax.swing.event.TreeSelectionListener;

/**
 * Which nodes of a tree are chosen.
 *
 * <h2>Paths are kept, not rows</h2>
 *
 * <p>A path is still the same if something further up is expanded or collapsed; a row is not.
 * That is why the selection is {@link TreePath}s, and the rows are computed when needed by asking
 * the {@link RowMapper}. See that interface's note.
 *
 * <h2>Three modes, and the middle one is the odd one</h2>
 *
 * <p>One only, several contiguous, or any. The contiguous one asks that the chosen rows be
 * consecutive, and that depends on what is expanded: collapsing a node in the middle may make a
 * selection that was not contiguous become so. It is the reason {@link #resetRowSelection}
 * exists, which the view calls when what is expanded changes.
 */
public interface TreeSelectionModel {

    /** A single node. */
    int SINGLE_TREE_SELECTION = 1;

    /** Several, but on consecutive rows. */
    int CONTIGUOUS_TREE_SELECTION = 2;

    /** Any set. */
    int DISCONTIGUOUS_TREE_SELECTION = 4;

    void setSelectionMode(int mode);

    int getSelectionMode();

    /** It leaves only that path chosen. */
    void setSelectionPath(TreePath path);

    void setSelectionPaths(TreePath[] paths);

    void addSelectionPath(TreePath path);

    void addSelectionPaths(TreePath[] paths);

    void removeSelectionPath(TreePath path);

    void removeSelectionPaths(TreePath[] paths);

    /** The first of the chosen ones, or null. */
    TreePath getSelectionPath();

    TreePath[] getSelectionPaths();

    int getSelectionCount();

    boolean isPathSelected(TreePath path);

    boolean isSelectionEmpty();

    void clearSelection();

    /** Who translates paths to rows; see the interface note. */
    void setRowMapper(RowMapper newMapper);

    RowMapper getRowMapper();

    int[] getSelectionRows();

    int getMinSelectionRow();

    int getMaxSelectionRow();

    boolean isRowSelected(int row);

    /** It computes the rows again; the view calls it when what is expanded changes. */
    void resetRowSelection();

    int getLeadSelectionRow();

    TreePath getLeadSelectionPath();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    void addTreeSelectionListener(TreeSelectionListener x);

    void removeTreeSelectionListener(TreeSelectionListener x);
}
