package com.sun.source.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;

/**
 * A documentation node and everything that contains it: as far as the comment, and from there
 * as far as the code.
 *
 * <h2>The two chained trees</h2>
 *
 * <p>It is what tells this class from {@link TreePath}: a documentation path ends in a
 * {@link TreePath}, not in a root of its own. It has to be like that because the question that
 * is asked about a documentation node is almost always about the code -- "which method does
 * this {@code @param} document" -- and that answer is on the other side of the border between
 * the two trees.
 */
public class DocTreePath implements Iterable<DocTree> {

    private final TreePath treePath;
    private final DocCommentTree docComment;
    private final DocTree leaf;
    private final DocTreePath parent;

    /**
     * The path as far as {@code target} inside that comment, or {@code null} if it is not there.
     */
    public static DocTreePath getPath(TreePath treePath, DocCommentTree comment, DocTree target) {
        return getPath(new DocTreePath(treePath, comment), target);
    }

    /** The same, starting from a path that is already built. */
    public static DocTreePath getPath(DocTreePath path, DocTree target) {
        if (path == null || target == null) {
            throw new NullPointerException("path and target may not be null");
        }
        return new Finder(target).find(path);
    }

    /** The path that is only the comment. */
    public DocTreePath(TreePath treePath, DocCommentTree t) {
        if (treePath == null || t == null) {
            throw new NullPointerException("treePath and comment may not be null");
        }
        this.treePath = treePath;
        this.docComment = t;
        this.leaf = t;
        this.parent = null;
    }

    /** {@code p}'s path extended with {@code t}. */
    public DocTreePath(DocTreePath p, DocTree t) {
        if (t.getKind() == DocTree.Kind.DOC_COMMENT) {
            throw new IllegalArgumentException("a DocCommentTree is the root, not a leaf");
        }
        this.treePath = p.treePath;
        this.docComment = p.docComment;
        this.leaf = t;
        this.parent = p;
    }

    /** The path in the code tree where this comment lives. */
    public TreePath getTreePath() {
        return this.treePath;
    }

    /** The whole comment. */
    public DocCommentTree getDocComment() {
        return this.docComment;
    }

    /** The node at the end. */
    public DocTree getLeaf() {
        return this.leaf;
    }

    /** The path as far as the parent, or {@code null} if this is the comment. */
    public DocTreePath getParentPath() {
        return this.parent;
    }

    /** From the node towards the comment. */
    public Iterator<DocTree> iterator() {
        return new Upwards(this);
    }

    private static final class Upwards implements Iterator<DocTree> {

        private DocTreePath current;

        Upwards(DocTreePath from) {
            this.current = from;
        }

        public boolean hasNext() {
            return this.current != null;
        }

        public DocTree next() {
            if (this.current == null) {
                throw new NoSuchElementException();
            }
            DocTree t = this.current.leaf;
            this.current = this.current.parent;
            return t;
        }
    }

    /** See {@link TreePath}'s finder note: it stops with an exception on finding it. */
    private static final class Finder extends DocTreePathScanner<DocTreePath, Void> {

        private final DocTree target;

        Finder(DocTree target) {
            this.target = target;
        }

        DocTreePath find(DocTreePath from) {
            try {
                scan(from, null);
                return null;
            } catch (Found e) {
                return e.path;
            }
        }

        public DocTreePath scan(DocTree node, Void p) {
            if (node == this.target) {
                throw new Found(new DocTreePath(getCurrentPath(), node));
            }
            return super.scan(node, p);
        }
    }

    private static final class Found extends RuntimeException {

        private static final long serialVersionUID = 1L;

        final transient DocTreePath path;

        Found(DocTreePath path) {
            super(null, null, false, false);
            this.path = path;
        }
    }
}
