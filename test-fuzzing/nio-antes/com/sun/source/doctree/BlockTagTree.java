package com.sun.source.doctree;

/**
 * The half of the hierarchy that groups the **block** tags: those that go alone on
 * a line, starting with `@`, after the comment's body. See {@link InlineTagTree} for the other
 * half and for why they are separate.
 */
public interface BlockTagTree extends DocTree {

    String getTagName();
}
