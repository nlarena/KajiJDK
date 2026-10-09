package javax.swing.tree;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * What {@link FixedHeightLayoutCache} and {@link VariableHeightLayoutCache} have in common:
 * which nodes are seen and in what order.
 *
 * <p>It is not public. The JDK's two caches are two different internal trees, each optimized for
 * its case; here the part that decides <em>which</em> the rows are is a single one, and the only
 * thing that tells them apart is how each one's <em>height</em> is computed. Separating the two
 * questions is what allows the translation between rows and paths to be tested without a
 * screen.
 *
 * <p>The list of visible nodes is built whole when needed and discarded when something changes.
 * It is simpler than keeping it up to date and gives exactly the same from outside; a tree with
 * a hundred thousand expanded nodes would notice, one that fits a screen would not.
 */
final class LayoutCacheCore {

    private final Set<TreePath> expanded = new HashSet<TreePath>();
    private List<TreePath> visibles;
    private TreeModel model;
    private boolean rootVisible;

    void setTreeModel(TreeModel m) {
        this.model = m;
        this.expanded.clear();
        invalidate();
    }

    TreeModel getTreeModel() {
        return model;
    }

    void setRootVisible(boolean b) {
        this.rootVisible = b;
        invalidate();
    }

    /** It throws the list away; it is rebuilt on the next query. */
    void invalidate() {
        visibles = null;
    }

    /**
     * Marks that path as expanded or collapsed.
     *
     * <p>Expanding also expands all its parents: a node expanded inside a collapsed one would not
     * be seen, and whoever asks to expand it wants to see it.
     */
    void setExpanded(TreePath path, boolean expand) {
        if (path == null) {
            return;
        }
        if (expand) {
            TreePath p = path;
            while (p != null) {
                expanded.add(p);
                p = p.getParentPath();
            }
        } else {
            expanded.remove(path);
        }
        invalidate();
    }

    /** Whether that path is marked as expanded. */
    boolean isMarkedExpanded(TreePath path) {
        return path != null && expanded.contains(path);
    }

    /** Whether that path and all its parents are expanded. */
    boolean reallyExpanded(TreePath path) {
        TreePath p = path;
        while (p != null) {
            if (!expanded.contains(p)) {
                return false;
            }
            p = p.getParentPath();
        }
        return true;
    }

    /** The rows, in order. */
    List<TreePath> list() {
        if (visibles == null) {
            visibles = new ArrayList<TreePath>();
            if (model != null) {
                Object root = model.getRoot();
                if (root != null) {
                    TreePath path = new TreePath(root);
                    if (rootVisible) {
                        visibles.add(path);
                        addChildren(path);
                    } else {
                        // The hidden root takes up no row, but its children do -- and they are
                        // shown even
                                                // though nobody expanded it, because otherwise the
                                                // tree would look empty.
                        addChildrenOfHiddenRoot(path);
                    }
                }
            }
        }
        return visibles;
    }

    private void addChildren(TreePath parent) {
        if (!expanded.contains(parent)) {
            return;
        }
        Object node = parent.getLastPathComponent();
        int n = model.getChildCount(node);
        for (int i = 0; i < n; i++) {
            TreePath child = parent.pathByAddingChild(model.getChild(node, i));
            visibles.add(child);
            addChildren(child);
        }
    }

    private void addChildrenOfHiddenRoot(TreePath root) {
        Object node = root.getLastPathComponent();
        int n = model.getChildCount(node);
        for (int i = 0; i < n; i++) {
            TreePath child = root.pathByAddingChild(model.getChild(node, i));
            visibles.add(child);
            addChildren(child);
        }
    }

    int howMany() {
        return list().size();
    }

    TreePath pathForRow(int row) {
        List<TreePath> v = list();
        if (row < 0 || row >= v.size()) {
            return null;
        }
        return v.get(row);
    }

    int rowForPath(TreePath path) {
        if (path == null) {
            return -1;
        }
        List<TreePath> v = list();
        for (int i = 0; i < v.size(); i++) {
            if (v.get(i).equals(path)) {
                return i;
            }
        }
        return -1;
    }

    /** How many rows that node's visible descendants take up. */
    int visibleChildren(TreePath path) {
        int row = rowForPath(path);
        if (row < 0) {
            if (!reallyExpanded(path)) {
                return 0;
            }
        }
        List<TreePath> v = list();
        int count = 0;
        for (int i = row + 1; i < v.size(); i++) {
            if (isDescendant(v.get(i), path)) {
                count = count + 1;
            } else {
                i = v.size();
            }
        }
        return count;
    }

    private static boolean isDescendant(TreePath candidate, TreePath de) {
        TreePath p = candidate.getParentPath();
        while (p != null) {
            if (p.equals(de)) {
                return true;
            }
            p = p.getParentPath();
        }
        return false;
    }

    /** The rows from that one downwards. */
    Enumeration<TreePath> from(TreePath path) {
        int row = rowForPath(path);
        if (row < 0) {
            return null;
        }
        return new FromRow(list(), row);
    }

    /** Walks the list from a row to the end. */
    private static class FromRow implements Enumeration<TreePath> {

        private final List<TreePath> v;
        private int i;

        FromRow(List<TreePath> v, int i) {
            this.v = v;
            this.i = i;
        }

        public boolean hasMoreElements() {
            return i < v.size();
        }

        public TreePath nextElement() {
            if (i >= v.size()) {
                throw new NoSuchElementException();
            }
            TreePath p = v.get(i);
            i = i + 1;
            return p;
        }
    }
}
