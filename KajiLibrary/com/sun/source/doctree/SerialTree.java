package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@serial`, which says whether a field goes into the serialized form.
 */
public interface SerialTree extends BlockTagTree {

    List<? extends DocTree> getDescription();
}
