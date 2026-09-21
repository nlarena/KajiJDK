package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@serialData`, which documents the serialization's format.
 */
public interface SerialDataTree extends BlockTagTree {

    List<? extends DocTree> getDescription();
}
