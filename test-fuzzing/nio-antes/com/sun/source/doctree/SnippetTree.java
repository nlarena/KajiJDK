package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `{@snippet}`, which inserts example code with attributes that
 * control where it comes from and how it is shown.
 */
public interface SnippetTree extends InlineTagTree {

    List<? extends DocTree> getAttributes();

    TextTree getBody();
}
