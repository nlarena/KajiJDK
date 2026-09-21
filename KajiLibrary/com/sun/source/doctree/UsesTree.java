package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@uses`, of a module's documentation.
 */
public interface UsesTree extends BlockTagTree {

    ReferenceTree getServiceType();

    List<? extends DocTree> getDescription();
}
