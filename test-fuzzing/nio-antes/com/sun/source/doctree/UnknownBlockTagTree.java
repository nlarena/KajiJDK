package com.sun.source.doctree;

import java.util.List;

/**
 * A block tag this tree does not know.
 *
 * <p>It exists because javadoc is extensible: a tool may define tags of its own, and the parser
 * has to be able to represent them without understanding them. Without this node, an unknown tag
 * would be a syntax error instead of an extension.
 */
public interface UnknownBlockTagTree extends BlockTagTree {

    List<? extends DocTree> getContent();
}
