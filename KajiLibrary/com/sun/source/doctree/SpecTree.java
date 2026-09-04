package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@spec`, que enlaza a una especificacion externa.
 */
public interface SpecTree extends BlockTagTree {

    TextTree getURL();

    List<? extends DocTree> getTitle();
}
