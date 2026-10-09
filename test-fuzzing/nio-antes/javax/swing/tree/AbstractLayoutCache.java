package javax.swing.tree;

import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.event.TreeModelEvent;

/**
 * The bookkeeping of which row corresponds to which node of the tree.
 *
 * <h2>Rows and paths are two different numberings</h2>
 *
 * <p>A {@link TreePath} identifies a node and never changes. A <em>row</em> is where that node
 * appears on the screen, and it changes every time something further up is expanded or
 * collapsed. Translating between the two is all this class does, and it is what allows the tree's
 * model to know nothing about screens.
 *
 * <h2>Collapsed nodes take up no row</h2>
 *
 * <p>A node exists in the model even if its parent is collapsed; it simply has no row. Hence
 * {@link #getRowForPath} returns -1 for what is not seen, and not an error: not being seen is a
 * normal state, not a mistake.
 *
 * <h2>Two implementations, and the difference is a single one</h2>
 *
 * <p>{@link FixedHeightLayoutCache} serves when every row measures the same -- then the row of a
 * pixel is a division -- and {@link VariableHeightLayoutCache} when not. Everything else is the
 * same, and that is why this class exists: so that the tree does not have to know which one it
 * got.
 *
 * <h2>Who measures</h2>
 *
 * <p>This class does not know how to draw or to measure text. It asks a {@link NodeDimensions},
 * which the look and feel sets on it. Without one, the measurements come out empty and the row
 * bookkeeping goes on working: separating the two things is precisely what allows the translation
 * to be tested without a screen.
 */
public abstract class AbstractLayoutCache implements RowMapper {

    /** Who knows how much each node takes up; see the class note. */
    protected NodeDimensions nodeDimensions;

    /** The tree's model. */
    protected TreeModel treeModel;

    /** The selection, so that the row bookkeeping can report to it. */
    protected TreeSelectionModel treeSelectionModel;

    /** Whether the root takes up a row. */
    protected boolean rootVisible;

    /** Each row's height, or zero if each one measures its own. */
    protected int rowHeight;

    /** For the subclasses. */
    protected AbstractLayoutCache() {
    }

    public void setNodeDimensions(NodeDimensions nd) {
        this.nodeDimensions = nd;
    }

    public NodeDimensions getNodeDimensions() {
        return nodeDimensions;
    }

    public void setModel(TreeModel newModel) {
        treeModel = newModel;
    }

    public TreeModel getModel() {
        return treeModel;
    }

    /**
     * Whether the root takes up a row.
     *
     * <p>With the root hidden, the root's children are the first-level rows. It is how a tree that
     * is really a forest is shown.
     */
    public void setRootVisible(boolean rootVisible) {
        this.rootVisible = rootVisible;
    }

    public boolean isRootVisible() {
        return rootVisible;
    }

    /**
     * Each row's height.
     *
     * <p>Zero or less means "each one measures its own", and it is what forces asking each node. A
     * positive number makes the row of a pixel a division.
     */
    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public void setSelectionModel(TreeSelectionModel newLSM) {
        if (treeSelectionModel != null) {
            treeSelectionModel.setRowMapper(null);
        }
        treeSelectionModel = newLSM;
        if (treeSelectionModel != null) {
            treeSelectionModel.setRowMapper(this);
        }
    }

    public TreeSelectionModel getSelectionModel() {
        return treeSelectionModel;
    }

    /** What all the rows take up together. */
    public int getPreferredHeight() {
        int rowCount = getRowCount();
        if (rowCount > 0) {
            Rectangle bounds = getBounds(getPathForRow(rowCount - 1), null);
            if (bounds != null) {
                return bounds.y + bounds.height;
            }
        }
        return 0;
    }

    /**
     * What the widest of the rows that fall in that rectangle takes up.
     *
     * <p>With a null rectangle it looks at them all. Looking only at the visible ones is what keeps
     * a tree of a hundred thousand nodes from having to measure them all to know how much
     * horizontal scrolling is needed.
     */
    public int getPreferredWidth(Rectangle bounds) {
        int rowCount = getRowCount();
        if (rowCount > 0) {
            int maxWidth = 0;
            int i = 0;
            int last = rowCount;
            if (bounds != null) {
                TreePath first = getPathClosestTo(bounds.x, bounds.y);
                i = (first == null) ? 0 : getRowForPath(first);
                if (i < 0) {
                    i = 0;
                }
                TreePath end = getPathClosestTo(bounds.x, bounds.y + bounds.height);
                int r = (end == null) ? rowCount - 1 : getRowForPath(end);
                last = (r < 0) ? rowCount : r + 1;
            }
            while (i < last) {
                Rectangle b = getBounds(getPathForRow(i), null);
                if (b != null && b.x + b.width > maxWidth) {
                    maxWidth = b.x + b.width;
                }
                i = i + 1;
            }
            return maxWidth;
        }
        return 0;
    }

    /** Whether that path is expanded. */
    public abstract boolean isExpanded(TreePath path);

    /** Where that node goes, or null if it is not seen. */
    public abstract Rectangle getBounds(TreePath path, Rectangle placeIn);

    /** That row's node, or null if that row does not exist. */
    public abstract TreePath getPathForRow(int row);

    /** That node's row, or -1 if it is not seen; see the class note. */
    public abstract int getRowForPath(TreePath path);

    /** The node nearest that point. */
    public abstract TreePath getPathClosestTo(int x, int y);

    /** The visible nodes from that one downwards. */
    public abstract Enumeration<TreePath> getVisiblePathsFrom(TreePath path);

    /** How many rows that node's visible descendants take up. */
    public abstract int getVisibleChildCount(TreePath path);

    /** Expands or collapses that path. */
    public abstract void setExpandedState(TreePath path, boolean isExpanded);

    /** Whether that path is expanded and so are all its parents. */
    public abstract boolean getExpandedState(TreePath path);

    /** How many rows there are. */
    public abstract int getRowCount();

    /** It forgets every measurement it had kept. */
    public abstract void invalidateSizes();

    /** It forgets that node's measurement. */
    public abstract void invalidatePathBounds(TreePath path);

    /** Those nodes changed. */
    public abstract void treeNodesChanged(TreeModelEvent e);

    /** Those nodes were added. */
    public abstract void treeNodesInserted(TreeModelEvent e);

    /** Those nodes were removed. */
    public abstract void treeNodesRemoved(TreeModelEvent e);

    /** The structure under that node changed. */
    public abstract void treeStructureChanged(TreeModelEvent e);

    /**
     * Those paths' rows.
     *
     * <p>It is what gives the selection model a {@link RowMapper}. An empty array if there are no
     * rows, not null: the caller is going to walk it.
     */
    public int[] getRowsForPaths(TreePath[] paths) {
        if (paths == null) {
            return null;
        }
        int numPaths = paths.length;
        int[] rows = new int[numPaths];
        for (int counter = 0; counter < numPaths; counter++) {
            rows[counter] = getRowForPath(paths[counter]);
        }
        return rows;
    }

    /** It asks the measurer; an empty rectangle if none is set. */
    protected Rectangle getNodeDimensions(Object value, int row, int depth,
            boolean expanded, Rectangle placeIn) {
        NodeDimensions nd = getNodeDimensions();
        if (nd != null) {
            return nd.getNodeDimensions(value, row, depth, expanded, placeIn);
        }
        return null;
    }

    /** Whether every row measures the same; see {@link #setRowHeight}. */
    protected boolean isFixedRowHeight() {
        return (rowHeight > 0);
    }

    /**
     * Who knows how much a drawn node takes up.
     *
     * <p>The look and feel sets it, because measuring depends on the typeface and on the icons,
     * which belong to the look and feel and not to the tree.
     */
    public abstract static class NodeDimensions {

        /** For the subclasses. */
        protected NodeDimensions() {
        }

        /**
         * The rectangle that node takes up.
         *
         * <p>It is passed a rectangle to fill so as to avoid creating one per node; with null it
         * returns a new one.
         */
        public abstract Rectangle getNodeDimensions(Object value, int row, int depth,
                boolean expanded, Rectangle bounds);
    }
}
