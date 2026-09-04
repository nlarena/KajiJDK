package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@param`. {@link #isTypeParameter} distingue `@param x` de
 * `@param <T>`, que se escriben casi igual y documentan cosas completamente distintas.
 */
public interface ParamTree extends BlockTagTree {

    /** Si documenta un parametro de tipo (`@param <T>`) y no uno comun. */
    boolean isTypeParameter();

    IdentifierTree getName();

    List<? extends DocTree> getDescription();
}
