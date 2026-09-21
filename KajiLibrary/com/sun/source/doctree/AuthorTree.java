package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@author`. The name comes as a list of nodes and not as text because
 * an author may carry an `<a href=...>` inside.
 */
public interface AuthorTree extends BlockTagTree {

    List<? extends DocTree> getName();
}
