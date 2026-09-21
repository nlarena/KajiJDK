package com.sun.source.tree;

/**
 * A module's `requires` directive, with its two flags.
 */
public interface RequiresTree extends DirectiveTree {

    /** Whether the module is needed in order to compile but not in order to run. */
    boolean isStatic();

    /** Whether whoever requires this module also reads the required one. */
    boolean isTransitive();

    ExpressionTree getModuleName();
}
