package com.sun.source.tree;

import java.util.List;

/**
 * El tipo union de un `catch` multiple, `A | B`.
 */
public interface UnionTypeTree extends Tree {

    List<? extends Tree> getTypeAlternatives();
}
