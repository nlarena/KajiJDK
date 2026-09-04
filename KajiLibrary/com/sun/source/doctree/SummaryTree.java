package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `{@summary}`, que dice explicitamente cual es la primera oracion
 * en vez de dejar que la herramienta la adivine buscando el primer punto.
 */
public interface SummaryTree extends InlineTagTree {

    List<? extends DocTree> getSummary();
}
