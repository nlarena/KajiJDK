package com.sun.source.doctree;

import java.util.List;

/**
 * The node of `@version`.
 */
public interface VersionTree extends BlockTagTree {

    List<? extends DocTree> getBody();
}
