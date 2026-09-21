package javax.swing.tree;

/**
 * Translates tree paths to row numbers.
 *
 * <h2>Why the selection model does not know it</h2>
 *
 * <p>A tree's selection is kept as paths, not as rows: a path is still the same if something
 * further up is expanded, and a row is not. But to draw, the row has to be known, and that
 * depends on what is expanded, which is the view's business.
 *
 * <p>This interface is the bridge. The selection model has one set and asks it when it is asked
 * for rows; a path that is not seen -- because its parent is collapsed -- returns -1.
 */
public interface RowMapper {

    /** Those paths' rows; -1 for those that are not seen. */
    int[] getRowsForPaths(TreePath[] path);
}
