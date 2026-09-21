package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@see`, whose content may be a reference, a string between
 * quotes or an `<a href>` -- that is why it is a list of nodes and not a reference.
 */
public interface SeeTree extends BlockTagTree {

    List<? extends DocTree> getReference();
}
