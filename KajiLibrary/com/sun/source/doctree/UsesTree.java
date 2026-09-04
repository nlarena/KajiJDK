package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@uses`, de la documentacion de un modulo.
 */
public interface UsesTree extends BlockTagTree {

    ReferenceTree getServiceType();

    List<? extends DocTree> getDescription();
}
