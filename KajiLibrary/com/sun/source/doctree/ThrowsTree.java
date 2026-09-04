package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@throws` y de `@exception`, que son sinonimos y se distinguen por el
 * {@link #getTagName}.
 */
public interface ThrowsTree extends BlockTagTree {

    ReferenceTree getExceptionName();

    List<? extends DocTree> getDescription();
}
