package com.sun.source.doctree;

/**
 * Plain text. The commonest leaf of the tree.
 */
public interface TextTree extends DocTree {

    String getBody();
}
