package com.sun.source.tree;

import java.util.List;

/**
 * A module's `exports` directive. {@link #getModuleNames} is the list of the
 * `to`, empty when the export is to everybody -- which is the distinction that decides whether
 * a package is public API.
 */
public interface ExportsTree extends DirectiveTree {

    ExpressionTree getPackageName();

    /** The modules of the `to`, or empty if it is exported to everybody. */
    List<? extends ExpressionTree> getModuleNames();
}
