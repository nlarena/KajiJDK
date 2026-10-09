package com.sun.source.tree;

import java.util.List;

/**
 * A `{ ... }` block. {@link #isStatic} tells it apart from a static initializer,
 * which has the same shape.
 */
public interface BlockTree extends StatementTree {

    /** Whether it is a static initializer and not a common block. */
    boolean isStatic();

    List<? extends StatementTree> getStatements();
}
