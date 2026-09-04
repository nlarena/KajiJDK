package com.sun.source.tree;

/**
 * Un comodin `?`, `? extends T` o `? super T`. Cual de los tres lo dice
 * {@link Tree#getKind}.
 */
public interface WildcardTree extends Tree {

    Tree getBound();
}
