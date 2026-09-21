package javax.swing.tree;

import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.event.TreeModelEvent;

/**
 * The row bookkeeping for a tree where each row measures its own.
 *
 * <h2>What tells it apart from the other one</h2>
 *
 * <p>{@link FixedHeightLayoutCache} gets a row's position with a multiplication. This one has to
 * ask each node how much it measures and add up from the top, which makes it more expensive and
 * the only one that serves when the rows have icons of different sizes or text of several lines.
 *
 * <p>The other half -- which nodes are seen and in what order -- is identical; see
 * {@link AbstractLayoutCache}'s note.
 *
 * <h2>Without a measurer there are no heights</h2>
 *
 * <p>If nobody set an {@link AbstractLayoutCache.NodeDimensions} and there is no fixed height,
 * there is nowhere to get how much a row measures from: {@link #getBounds} returns an empty
 * rectangle -- and not null, which is what the other cache does -- and the row bookkeeping goes
 * on working. One can translate between rows and paths without having drawn anything.
 */
public class VariableHeightLayoutCache extends AbstractLayoutCache {

    private final LayoutCacheCore core = new LayoutCacheCore();

    /** An empty cache. */
    public VariableHeightLayoutCache() {
        super();
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
        if (rowHeight != getRowHeight()) {
            super.setRowHeight(rowHeight);
            invalidateSizes();
        }
    }

    public void setNodeDimensions(NodeDimensions nd) {
        super.setNodeDimensions(nd);
        invalidateSizes();
    }

    public void setExpandedState(TreePath path, boolean isExpanded) {
        core.setExpanded(path, isExpanded);
    }

    public boolean getExpandedState(TreePath path) {
        return core.reallyExpanded(path);
    }

    /**
     * Where that node goes.
     *
     * <p>The vertical position is accumulated by adding up what each previous row measures. With a
     * fixed height set, that one is used and nothing is asked.
     */
    public Rectangle getBounds(TreePath path, Rectangle placeIn) {
        int row = getRowForPath(path);
        if (row < 0) {
            return null;
        }
        Rectangle r = measure(row, placeIn);
        if (r == null) {
            // Without a measurer there is no width, but there is a row: an empty rectangle is
            // returned
                        // and not null. It is measured, and it is different from what the
                        // fixed-height cache does, which does return null. The asymmetry is the
                        // JDK's.
            r = (placeIn != null) ? placeIn : new Rectangle();
            r.x = 0;
            r.width = 0;
            r.height = 0;
        }
        r.y = above(row);
        if (isFixedRowHeight()) {
            r.height = getRowHeight();
        }
        return r;
    }

    /** What that row takes up, asking the measurer. */
    private Rectangle measure(int row, Rectangle placeIn) {
        TreePath path = core.pathForRow(row);
        if (path == null) {
            return null;
        }
        return getNodeDimensions(path.getLastPathComponent(), row, path.getPathCount() - 1,
                core.isMarkedExpanded(path), placeIn);
    }

    /** Where that row starts: the sum of what the previous ones measure. */
    private int above(int row) {
        if (isFixedRowHeight()) {
            return row * getRowHeight();
        }
        int y = 0;
        for (int i = 0; i < row; i++) {
            Rectangle r = measure(i, null);
            if (r != null) {
                y = y + r.height;
            }
        }
        return y;
    }

    public TreePath getPathForRow(int row) {
        return core.pathForRow(row);
    }

    public int getRowForPath(TreePath path) {
        return core.rowForPath(path);
    }

    public int getRowCount() {
        return core.howMany();
    }

    /** It keeps no measurements: every query asks again. */
    public void invalidatePathBounds(TreePath path) {
    }

    /** The same; see {@link #invalidatePathBounds}. */
    public void invalidateSizes() {
    }

    public int getPreferredHeight() {
        int n = getRowCount();
        if (n == 0) {
            return 0;
        }
        if (isFixedRowHeight()) {
            return n * getRowHeight();
        }
        int y = 0;
        for (int i = 0; i < n; i++) {
            Rectangle r = measure(i, null);
            if (r != null) {
                y = y + r.height;
            }
        }
        return y;
    }

    public int getPreferredWidth(Rectangle bounds) {
        return super.getPreferredWidth(bounds);
    }

    /**
     * The node nearest that point.
     *
     * <p>As in the other cache, the horizontal coordinate is not looked at.
     */
    public TreePath getPathClosestTo(int x, int y) {
        int n = getRowCount();
        if (n == 0) {
            return null;
        }
        if (y < 0) {
            return getPathForRow(0);
        }
        if (isFixedRowHeight()) {
            int row = y / getRowHeight();
            if (row >= n) {
                row = n - 1;
            }
            return getPathForRow(row);
        }
        int accumulated = 0;
        for (int i = 0; i < n; i++) {
            Rectangle r = measure(i, null);
            int height = (r == null) ? 0 : r.height;
            if (y < accumulated + height) {
                return getPathForRow(i);
            }
            accumulated = accumulated + height;
        }
        return getPathForRow(n - 1);
    }

    public Enumeration<TreePath> getVisiblePathsFrom(TreePath path) {
        return core.from(path);
    }

    public int getVisibleChildCount(TreePath path) {
        return core.visibleChildren(path);
    }

    public boolean isExpanded(TreePath path) {
        return core.isMarkedExpanded(path);
    }

    /** A node that changes may change height, so everything below it shifts. */
    public void treeNodesChanged(TreeModelEvent e) {
        core.invalidate();
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
