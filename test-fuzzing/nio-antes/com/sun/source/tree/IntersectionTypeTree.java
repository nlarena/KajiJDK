package com.sun.source.tree;

import java.util.List;

/**
 * An intersection type `A & B`, which appears in the bounds of a type
 * parameter and in some casts.
 */
public interface IntersectionTypeTree extends Tree {

    List<? extends Tree> getBounds();
}
