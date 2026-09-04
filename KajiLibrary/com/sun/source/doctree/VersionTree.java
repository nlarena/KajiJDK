package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@version`.
 */
public interface VersionTree extends BlockTagTree {

    List<? extends DocTree> getBody();
}
