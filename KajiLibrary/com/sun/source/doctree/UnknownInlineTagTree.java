package com.sun.source.doctree;

import java.util.List;

/**
 * The same as {@link UnknownBlockTagTree}, for an inline tag.
 */
public interface UnknownInlineTagTree extends InlineTagTree {

    List<? extends DocTree> getContent();
}
