package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@provides`, de la documentacion de un modulo.
 */
public interface ProvidesTree extends BlockTagTree {

    ReferenceTree getServiceType();

    List<? extends DocTree> getDescription();
}
