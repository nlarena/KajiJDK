package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@return` and of `{@return}`.
 *
 * <p>The only node of the package that implements **both** tag hierarchies, because `@return`
 * exists in the two forms: as a block tag at the end, and from Java 16 on also inline at the
 * beginning of the description. {@link #isInline} says which one was written.
 */
public interface ReturnTree extends BlockTagTree, InlineTagTree {

    /** Whether it was written as `{@return ...}` and not as `@return ...`. */
    default boolean isInline() {
        return false;
    }

    List<? extends DocTree> getDescription();
}
