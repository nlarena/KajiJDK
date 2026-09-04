package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@see`, cuyo contenido puede ser una referencia, una cadena entre
 * comillas o un `<a href>` — por eso es una lista de nodos y no una referencia.
 */
public interface SeeTree extends BlockTagTree {

    List<? extends DocTree> getReference();
}
