package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `{@snippet}`, que inserta codigo de ejemplo con atributos que
 * controlan de donde sale y como se muestra.
 */
public interface SnippetTree extends InlineTagTree {

    List<? extends DocTree> getAttributes();

    TextTree getBody();
}
