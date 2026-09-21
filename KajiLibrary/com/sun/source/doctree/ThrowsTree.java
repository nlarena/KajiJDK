package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@throws` and of `@exception`, which are synonyms and are told apart by
 * the {@link #getTagName}.
 */
public interface ThrowsTree extends BlockTagTree {

    ReferenceTree getExceptionName();

    List<? extends DocTree> getDescription();
}
