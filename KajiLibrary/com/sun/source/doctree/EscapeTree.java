package com.sun.source.doctree;

/**
 * Un escape de Markdown, como `\\*`. El {@link #getBody} es lo que el escape
 * representa, ya sin la barra.
 */
public interface EscapeTree extends TextTree {

    String getBody();
}
