package com.sun.source.doctree;

/**
 * La declaracion `<!DOCTYPE ...>` de un archivo de documentacion suelto.
 */
public interface DocTypeTree extends DocTree {

    String getText();
}
