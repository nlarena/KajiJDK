package com.sun.source.doctree;

/**
 * El nodo de `{@inheritDoc}`, que trae la documentacion del metodo sobrescrito.
 *
 * <p>{@link #getSupertype} es `null` en la forma sin argumento —la clasica— y devuelve la
 * referencia cuando se escribio `{@inheritDoc Supertipo}`, que existe para desambiguar cuando hay
 * mas de un supertipo con documentacion.
 */
public interface InheritDocTree extends InlineTagTree {

    /** El supertipo del que heredar, o `null` si no se dijo cual. */
    default ReferenceTree getSupertype() {
        return null;
    }
}
