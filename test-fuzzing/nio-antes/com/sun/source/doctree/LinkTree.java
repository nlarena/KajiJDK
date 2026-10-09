package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `{@link}` and of `{@linkplain}`, which are told apart by the
 * {@link #getTagName} and not by the type -- the difference is only typographical.
 */
public interface LinkTree extends InlineTagTree {

    ReferenceTree getReference();

    List<? extends DocTree> getLabel();
}
