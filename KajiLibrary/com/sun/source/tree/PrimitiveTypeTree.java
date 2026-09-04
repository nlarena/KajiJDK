package com.sun.source.tree;

import javax.lang.model.type.TypeKind;

/**
 * Un tipo primitivo escrito en el fuente. Reusa el {@code TypeKind} del
 * modelo de elementos en vez de definir su propio enum.
 */
public interface PrimitiveTypeTree extends Tree {

    TypeKind getPrimitiveTypeKind();
}
