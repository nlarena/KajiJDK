package com.sun.source.doctree;

/**
 * An HTML comment inside the javadoc. It is kept in the tree instead of being thrown
 * away: a tool may want to see it, and deleting it here would make it unrecoverable.
 */
public interface CommentTree extends DocTree {

    String getBody();
}
