package com.sun.source.tree;

/**
 * A wildcard `?`, `? extends T` or `? super T`. Which of the three it is is said by
 * {@link Tree#getKind}.
 */
public interface WildcardTree extends Tree {

    Tree getBound();
}
