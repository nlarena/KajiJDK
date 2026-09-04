package com.sun.source.tree;

import java.util.List;

/**
 * La directiva `exports` de un modulo. {@link #getModuleNames} es la lista del
 * `to`, vacia cuando la exportacion es a todos — que es la distincion que decide si un paquete es
 * API publica.
 */
public interface ExportsTree extends DirectiveTree {

    ExpressionTree getPackageName();

    /** Los modulos del `to`, o vacio si se exporta a todos. */
    List<? extends ExpressionTree> getModuleNames();
}
