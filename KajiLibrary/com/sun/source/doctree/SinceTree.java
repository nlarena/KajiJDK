package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@since`.
 */
public interface SinceTree extends BlockTagTree {

    List<? extends DocTree> getBody();
}
