package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@provides`, of a module's documentation.
 */
public interface ProvidesTree extends BlockTagTree {

    ReferenceTree getServiceType();

    List<? extends DocTree> getDescription();
}
