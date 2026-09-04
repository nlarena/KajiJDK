package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `{@index}`, que agrega un termino al indice de busqueda.
 */
public interface IndexTree extends InlineTagTree {

    DocTree getSearchTerm();

    List<? extends DocTree> getDescription();
}
