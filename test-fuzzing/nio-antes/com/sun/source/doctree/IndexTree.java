package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `{@index}`, which adds a term to the search index.
 */
public interface IndexTree extends InlineTagTree {

    DocTree getSearchTerm();

    List<? extends DocTree> getDescription();
}
