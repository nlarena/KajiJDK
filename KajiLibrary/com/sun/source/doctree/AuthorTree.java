package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@author`. El nombre viene como lista de nodos y no como texto porque
 * un autor puede llevar un `<a href=...>` adentro.
 */
public interface AuthorTree extends BlockTagTree {

    List<? extends DocTree> getName();
}
