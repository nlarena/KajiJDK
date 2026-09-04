package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@serialField`, que documenta un campo de la forma serializada.
 */
public interface SerialFieldTree extends BlockTagTree {

    IdentifierTree getName();

    ReferenceTree getType();

    List<? extends DocTree> getDescription();
}
