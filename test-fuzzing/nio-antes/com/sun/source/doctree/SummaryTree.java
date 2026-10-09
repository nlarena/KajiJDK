package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `{@summary}`, which says explicitly which the first sentence is
 * instead of letting the tool guess it by looking for the first full stop.
 */
public interface SummaryTree extends InlineTagTree {

    List<? extends DocTree> getSummary();
}
