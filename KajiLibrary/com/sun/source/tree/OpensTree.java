package com.sun.source.tree;

import java.util.List;

/**
 * A module's `opens` directive, which enables reflection instead of compilation.
 */
public interface OpensTree extends DirectiveTree {

    ExpressionTree getPackageName();

    List<? extends ExpressionTree> getModuleNames();
}
