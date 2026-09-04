package com.sun.source.doctree;

import java.util.List;

/**
 * El nodo de `{@link}` y de `{@linkplain}`, que se distinguen por el
 * {@link #getTagName} y no por el tipo — la diferencia es solo tipografica.
 */
public interface LinkTree extends InlineTagTree {

    ReferenceTree getReference();

    List<? extends DocTree> getLabel();
}
