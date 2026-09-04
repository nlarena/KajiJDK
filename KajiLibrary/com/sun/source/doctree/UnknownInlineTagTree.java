package com.sun.source.doctree;

import java.util.List;

/**
 * Lo mismo que {@link UnknownBlockTagTree}, para un tag en linea.
 */
public interface UnknownInlineTagTree extends InlineTagTree {

    List<? extends DocTree> getContent();
}
