package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@since`.
 */
public interface SinceTree extends BlockTagTree {

    List<? extends DocTree> getBody();
}
