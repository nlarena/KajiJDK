package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@param`. {@link #isTypeParameter} tells `@param x` from
 * `@param <T>`, which are written almost alike and document completely different things.
 */
public interface ParamTree extends BlockTagTree {

    /** Whether it documents a type parameter (`@param <T>`) and not a common one. */
    boolean isTypeParameter();

    IdentifierTree getName();

    List<? extends DocTree> getDescription();
}
