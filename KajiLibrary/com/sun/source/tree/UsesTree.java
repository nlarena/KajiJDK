package com.sun.source.tree;

/**
 * A module's `uses` directive.
 */
public interface UsesTree extends DirectiveTree {

    ExpressionTree getServiceName();
}
