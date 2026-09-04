package com.sun.source.tree;

/**
 * La directiva `uses` de un modulo.
 */
public interface UsesTree extends DirectiveTree {

    ExpressionTree getServiceName();
}
