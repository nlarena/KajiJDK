package com.sun.source.doctree;

/**
 * El nodo de `{@literal}` y de `{@code}`, que tampoco se distinguen por el tipo:
 * los dos escapan el HTML de adentro y el segundo ademas lo muestra en monoespaciado.
 */
public interface LiteralTree extends InlineTagTree {

    TextTree getBody();
}
