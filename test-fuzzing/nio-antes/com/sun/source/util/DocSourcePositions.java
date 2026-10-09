package com.sun.source.util;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.CompilationUnitTree;

/**
 * The same as {@link SourcePositions}, for the nodes of a documentation comment.
 *
 * <p><strong>Three</strong> arguments are needed and not two, and the reason is the same as
 * there taken one level further: a documentation node lives inside a comment, and the comment
 * inside a file. Without the comment in the middle nothing can be placed, because the same
 * documentation tree may be inherited and appear in several places.
 */
public interface DocSourcePositions extends SourcePositions {

    /** Where the documentation node starts, or {@code NOPOS}. */
    long getStartPosition(CompilationUnitTree file, DocCommentTree comment, DocTree tree);

    /** Where it ends, or {@code NOPOS}. */
    long getEndPosition(CompilationUnitTree file, DocCommentTree comment, DocTree tree);
}
