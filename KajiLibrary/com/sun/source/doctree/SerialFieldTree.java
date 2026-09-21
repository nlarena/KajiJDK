package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@serialField`, which documents a field of the serialized form.
 */
public interface SerialFieldTree extends BlockTagTree {

    IdentifierTree getName();

    ReferenceTree getType();

    List<? extends DocTree> getDescription();
}
