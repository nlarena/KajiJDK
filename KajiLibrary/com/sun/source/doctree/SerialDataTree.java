package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@serialData`, que documenta el formato de la serializacion.
 */
public interface SerialDataTree extends BlockTagTree {

    List<? extends DocTree> getDescription();
}
