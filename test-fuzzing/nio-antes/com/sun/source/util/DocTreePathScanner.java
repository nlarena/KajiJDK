package com.sun.source.util;

import com.sun.source.doctree.DocTree;

/**
 * A {@link DocTreeScanner} that keeps track of where it is.
 *
 * <p>The equivalent of {@link TreePathScanner} for the documentation tree, and with the same
 * purpose: that any visit may ask what contains it without walking the tree again.
 *
 * @param <R> what each visit returns
 * @param <P> the datum that is carried along
 */
public class DocTreePathScanner<R, P> extends DocTreeScanner<R, P> {

    private DocTreePath path;

    public DocTreePathScanner() {
    }

    /** It starts the walk from that path. */
    public R scan(DocTreePath path, P p) {
        this.path = path.getParentPath();
        try {
            return path.getLeaf().accept(this, p);
        } finally {
            this.path = null;
        }
    }

    /** It visits a node, pushing it onto the path while it lasts. */
    public R scan(DocTree tree, P p) {
        if (tree == null) {
            return null;
        }
        DocTreePath previous = this.path;
        if (previous != null) {
            this.path = new DocTreePath(previous, tree);
        }
        try {
            return tree.accept(this, p);
        } finally {
            this.path = previous;
        }
    }

    /** Where the walk is standing now. */
    public DocTreePath getCurrentPath() {
        return this.path;
    }
}
