package com.sun.source.doctree;

/**
 * El nodo de `{@value}`, que sustituye el valor de una constante.
 * {@link #getFormat} es el formato opcional que se le puede dar.
 */
public interface ValueTree extends InlineTagTree {

    ReferenceTree getReference();

    default TextTree getFormat() {
        return null;
    }
}
