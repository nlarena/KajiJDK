package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@hidden`, que saca al elemento de la documentacion generada.
 */
public interface HiddenTree extends BlockTagTree {

    List<? extends DocTree> getBody();
}
