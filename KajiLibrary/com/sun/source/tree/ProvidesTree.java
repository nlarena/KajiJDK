package com.sun.source.tree;

import java.util.List;

/**
 * La directiva `provides ... with ...` de un modulo.
 */
public interface ProvidesTree extends DirectiveTree {

    ExpressionTree getServiceName();

    List<? extends ExpressionTree> getImplementationNames();
}
