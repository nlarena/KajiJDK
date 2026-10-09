package com.sun.source.doctree;

/**
 * Text in a format this tree does not interpret -- today, Markdown.
 */
public interface RawTextTree extends DocTree {

    String getContent();
}
