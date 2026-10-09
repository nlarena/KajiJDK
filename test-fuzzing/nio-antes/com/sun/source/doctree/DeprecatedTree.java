package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@deprecated`, with the text that explains why.
 */
public interface DeprecatedTree extends BlockTagTree {

    List<? extends DocTree> getBody();
}
