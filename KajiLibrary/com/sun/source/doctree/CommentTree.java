package com.sun.source.doctree;

/**
 * Un comentario HTML dentro del javadoc. Se conserva en el arbol en vez de tirarse:
 * una herramienta puede querer verlo, y borrarlo aca lo haria irrecuperable.
 */
public interface CommentTree extends DocTree {

    String getBody();
}
