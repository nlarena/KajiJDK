package com.sun.source.tree;

import java.util.List;

/**
 * Un tipo interseccion `A & B`, que aparece en los limites de un
 * parametro de tipo y en algunos casts.
 */
public interface IntersectionTypeTree extends Tree {

    List<? extends Tree> getBounds();
}
