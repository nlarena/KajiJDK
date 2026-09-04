package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@serial`, que dice si un campo entra en la forma serializada.
 */
public interface SerialTree extends BlockTagTree {

    List<? extends DocTree> getDescription();
}
