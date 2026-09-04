package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@deprecated`, con el texto que explica por que.
 */
public interface DeprecatedTree extends BlockTagTree {

    List<? extends DocTree> getBody();
}
