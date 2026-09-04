package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `@return` y de `{@return}`.
 *
 * <p>El unico nodo del paquete que implementa las **dos** jerarquias de tag, porque `@return`
 * existe en las dos formas: como tag de bloque al final, y desde Java 16 tambien en linea al
 * principio de la descripcion. {@link #isInline} dice cual se escribio.
 */
public interface ReturnTree extends BlockTagTree, InlineTagTree {

    /** Si se escribio como `{@return ...}` y no como `@return ...`. */
    default boolean isInline() {
        return false;
    }

    List<? extends DocTree> getDescription();
}
