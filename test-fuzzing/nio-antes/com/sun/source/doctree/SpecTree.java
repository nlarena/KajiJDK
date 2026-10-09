package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@spec`, which links to an external specification.
 */
public interface SpecTree extends BlockTagTree {

    TextTree getURL();

    List<? extends DocTree> getTitle();
}
