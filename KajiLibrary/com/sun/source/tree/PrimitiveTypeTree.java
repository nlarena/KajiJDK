package com.sun.source.tree;

import javax.lang.model.type.TypeKind;

/**
 * A primitive type written in the source. It reuses the {@code TypeKind} of the
 * element model instead of defining an enum of its own.
 */
public interface PrimitiveTypeTree extends Tree {

    TypeKind getPrimitiveTypeKind();
}
