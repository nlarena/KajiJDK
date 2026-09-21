package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@hidden`, which takes the element out of the generated documentation.
 */
public interface HiddenTree extends BlockTagTree {

    List<? extends DocTree> getBody();
}
