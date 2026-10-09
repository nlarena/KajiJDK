package com.sun.source.doctree;

/**
 * A Markdown escape, such as `\\*`. The {@link #getBody} is what the escape
 * represents, already without the slash.
 */
public interface EscapeTree extends TextTree {

    String getBody();
}
