package javax.swing.tree;

import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.event.TreeModelEvent;

/**
 * The row bookkeeping for a tree where every row measures the same.
 *
 * <h2>Why there are two caches</h2>
 *
 * <p>With every row the same height, knowing which row a pixel falls in is a division and knowing
 * where a row starts is a multiplication. That holds for a tree of any size and without measuring
 * a single node. The other cache -- {@link VariableHeightLayoutCache} -- has to ask each node how
 * much it measures and add up.
 *
 * <p>The rest -- which nodes are seen, in what order, which row belongs to which -- is the same
 * in both.
 */
public class FixedHeightLayoutCache extends AbstractLayoutCache {

    private final LayoutCacheCore core = new LayoutCacheCore();

    /** An empty cache. */
    public FixedHeightLayoutCache() {
        super();
        setRowHeight(1);
    }

    public void setModel(TreeModel newModel) {
        super.setModel(newModel);
        core.setTreeModel(newModel);
        if (newModel != null && newModel.getRoot() != null) {
            setExpandedState(new TreePath(newModel.getRoot()), true);
        }
    }

    public void setRootVisible(boolean rootVisible) {
        if (isRootVisible() != rootVisible) {
            super.setRootVisible(rootVisible);
            core.setRootVisible(rootVisible);
        }
    }

    public void setRowHeight(int rowHeight) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException(
                    "FixedHeightLayoutCache only supports row heights greater than 0");
        }
        if (getRowHeight() != rowHeight) {
            super.setRowHeight(rowHeight);
        }
    }

    public int getRowCount() {
        return core.howMany();
    }

    /** It does nothing: with a fixed height there is no kept measurement to throw away. */
    public void invalidatePathBounds(TreePath path) {
    }

    /** It does nothing; see {@link #invalidatePathBounds}. */
    public void invalidateSizes() {
    }

    public boolean isExpanded(TreePath path) {
        return core.isMarkedExpanded(path);
    }

    /**
     * Where that node goes.
     *
     * <p>The height and the vertical position come from the row; the width and the horizontal
     * position, from the measurer. With no measurer there is no width to give and it returns null.
     */
    public Rectangle getBounds(TreePath path, Rectangle placeIn) {
        int row = getRowForPath(path);
        if (row < 0) {
            return null;
        }
        Object node = path.getLastPathComponent();
        Rectangle r = getNodeDimensions(node, row, path.getPathCount() - 1,
                core.isMarkedExpanded(path), placeIn);
        if (r == null) {
            return null;
        }
        r.y = row * getRowHeight();
        r.height = getRowHeight();
        return r;
    }

    public TreePath getPathForRow(int row) {
        return core.pathForRow(row);
    }

    public int getRowForPath(TreePath path) {
        return core.rowForPath(path);
    }

    /**
     * The node nearest that point.
     *
     * <p>The horizontal coordinate is not looked at: a row takes up the whole width, even if its
     * drawing does not. A click to the right of the text is still a click on that row.
     */
    public TreePath getPathClosestTo(int x, int y) {
        int n = getRowCount();
        if (n == 0) {
            return null;
        }
        int row = y / getRowHeight();
        if (row < 0) {
            row = 0;
        } else if (row >= n) {
            row = n - 1;
        }
        return getPathForRow(row);
    }

    public int getVisibleChildCount(TreePath path) {
        return core.visibleChildren(path);
    }

    public Enumeration<TreePath> getVisiblePathsFrom(TreePath path) {
        return core.from(path);
    }

    public void setExpandedState(TreePath path, boolean isExpanded) {
        core.setExpanded(path, isExpanded);
    }

    public boolean getExpandedState(TreePath path) {
        return core.reallyExpanded(path);
    }

    /** A node that changes changes no row: with a fixed height, nothing moves. */
    public void treeNodesChanged(TreeModelEvent e) {
    }

    public void treeNodesInserted(TreeModelEvent e) {
        core.invalidate();
    }

    public void treeNodesRemoved(TreeModelEvent e) {
        core.invalidate();
    }

    public void treeStructureChanged(TreeModelEvent e) {
        core.invalidate();
    }
}
