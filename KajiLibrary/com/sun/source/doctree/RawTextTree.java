package com.sun.source.doctree;

/**
 * Texto en un formato que este arbol no interpreta — hoy, Markdown.
 */
public interface RawTextTree extends DocTree {

    String getContent();
}
