package com.sun.source.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

/**
 * Where a node starts and ends inside the file.
 *
 * <h2>Why it does not live in the node</h2>
 *
 * <p>Because a position only means something <strong>inside a compilation unit</strong>, and a
 * node does not know which it is in: the trees may be shared and reused. Hence the two methods
 * ask for the {@link CompilationUnitTree} and are not getters of the node.
 *
 * <p>The positions are absolute -- a single number from the beginning of the file -- and not
 * line and column. Converting them is {@link com.sun.source.tree.LineMap}'s work, and it is
 * done only when showing something to a person.
 */
public interface SourcePositions {

    /**
     * Where it starts, or {@link javax.tools.Diagnostic#NOPOS} if the node did not come from the
     * source -- a synthetic node the compiler added is not written anywhere.
     */
    long getStartPosition(CompilationUnitTree file, Tree tree);

    /** Where it ends, or {@code NOPOS}. */
    long getEndPosition(CompilationUnitTree file, Tree tree);
}
