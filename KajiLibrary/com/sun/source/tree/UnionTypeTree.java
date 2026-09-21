package com.sun.source.tree;

import java.util.List;

/**
 * The union type of a multiple `catch`, `A | B`.
 */
public interface UnionTypeTree extends Tree {

    List<? extends Tree> getTypeAlternatives();
}
