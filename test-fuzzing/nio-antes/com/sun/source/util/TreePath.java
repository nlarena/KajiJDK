package com.sun.source.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

/**
 * A node and the chain of those that contain it, up to the root.
 *
 * <h2>Why it is needed</h2>
 *
 * <p>Because a {@link Tree} <strong>does not know its parent</strong>. That is deliberate: with
 * no upward pointers, a subtree may be shared between several places and the whole tree is
 * cheaper to build. The price is that "which class is this method in" cannot be answered from
 * the node.
 *
 * <p>This class is that context, built for a concrete walk. Hence it is immutable and
 * {@link #getParentPath} returns another path instead of a node: what is walked upwards is the
 * path, not the tree.
 *
 * <p>The iteration goes <strong>from the node towards the root</strong>, which is the useful
 * direction: almost every question is "which is the nearest X that contains me".
 */
public class TreePath implements Iterable<Tree> {

    private final CompilationUnitTree unit;
    private final Tree leaf;
    private final TreePath parent;

    /**
     * The path from {@code unit} as far as {@code target}, or {@code null} if it is not inside.
     *
     * <p>It walks the tree looking for it, so it is not free: for many queries a
     * {@link TreePathScanner} is best, which keeps it while it goes down.
     */
    public static TreePath getPath(CompilationUnitTree unit, Tree target) {
        return getPath(new TreePath(unit), target);
    }

    /** The same, starting from a path that is already built. */
    public static TreePath getPath(TreePath path, Tree target) {
        if (path == null || target == null) {
            throw new NullPointerException("path and target may not be null");
        }
        Finder b = new Finder(target);
        return b.find(path);
    }

    /** The path that is only the root. */
    public TreePath(CompilationUnitTree unit) {
        this.unit = unit;
        this.leaf = unit;
        this.parent = null;
    }

    /** {@code path}'s path extended with {@code leaf}. */
    public TreePath(TreePath path, Tree leaf) {
        this.unit = path.unit;
        this.leaf = leaf;
        this.parent = path;
    }

    /** The file it lives in. */
    public CompilationUnitTree getCompilationUnit() {
        return this.unit;
    }

    /** The node at the end. */
    public Tree getLeaf() {
        return this.leaf;
    }

    /** The path as far as the parent, or {@code null} if this is already the root. */
    public TreePath getParentPath() {
        return this.parent;
    }

    /** From the node towards the root. */
    public Iterator<Tree> iterator() {
        return new Upwards(this);
    }

    /** The iterator that goes up: see the class note about the direction. */
    private static final class Upwards implements Iterator<Tree> {

        private TreePath current;

        Upwards(TreePath from) {
            this.current = from;
        }

        public boolean hasNext() {
            return this.current != null;
        }

        public Tree next() {
            if (this.current == null) {
                throw new NoSuchElementException();
            }
            Tree t = this.current.leaf;
            this.current = this.current.parent;
            return t;
        }
    }

    /**
     * The walk that builds the path as far as a node.
     *
     * <p>It stops with an internal exception on finding it, and that is not abuse: the scanner
     * has no way of stopping halfway, and going on walking a whole file after having found what
     * was being looked for would be pure extra work.
     */
    private static final class Finder extends TreePathScanner<TreePath, Void> {

        private final Tree target;

        Finder(Tree target) {
            this.target = target;
        }

        TreePath find(TreePath from) {
            try {
                scan(from, null);
                return null;
            } catch (Found e) {
                return e.path;
            }
        }

        public TreePath scan(Tree node, Void p) {
            if (node == this.target) {
                throw new Found(new TreePath(getCurrentPath(), node));
            }
            return super.scan(node, p);
        }
    }

    /** The walk's cut-off; with no trace, because it is not an error but a result. */
    private static final class Found extends RuntimeException {

        private static final long serialVersionUID = 1L;

        final transient TreePath path;

        Found(TreePath path) {
            super(null, null, false, false);
            this.path = path;
        }
    }
}
