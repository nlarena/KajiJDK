package com.sun.source.util;

import com.sun.source.tree.Tree;

/**
 * A {@link TreeScanner} that also keeps track of where it is.
 *
 * <h2>What it contributes</h2>
 *
 * <p>{@link #getCurrentPath}. A bare {@code TreeScanner} sees one node at a time and does not
 * know what contains it; with this, any visit may ask which class or which method it is
 * standing in.
 *
 * <p>And it is cheap: the path is built while going down, instead of walking the tree again
 * with {@link TreePath#getPath}. Any query that needs context at more than one node is best
 * done this way.
 *
 * <p>The only rule when extending it: if {@code scan} is overridden, the base class's has to be
 * called -- it is the one that pushes and pops the path.
 *
 * @param <R> what each visit returns
 * @param <P> the datum that is carried along
 */
public class TreePathScanner<R, P> extends TreeScanner<R, P> {

    private TreePath path;

    public TreePathScanner() {
    }

    /** It starts the walk from that path, which is left as the initial context. */
    public R scan(TreePath path, P p) {
        this.path = path.getParentPath();
        try {
            return path.getLeaf().accept(this, p);
        } finally {
            this.path = null;
        }
    }

    /**
     * It visits a node, pushing it onto the path while it lasts.
     *
     * <p>The {@code finally} is not decoration: a visit may throw, and without restoring the path
     * the scanner would be left lying about where it is for everything that follows.
     */
    public R scan(Tree tree, P p) {
        if (tree == null) {
            return null;
        }
        TreePath previous = this.path;
        this.path = new TreePath(previous == null ? new TreePath(
                (com.sun.source.tree.CompilationUnitTree) tree) : previous, tree);
        try {
            return tree.accept(this, p);
        } finally {
            this.path = previous;
        }
    }

    /** Where the walk is standing now. */
    public TreePath getCurrentPath() {
        return this.path;
    }
}
