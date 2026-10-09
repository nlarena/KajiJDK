package com.sun.source.tree;

/**
 * A `case` label that is a pattern.
 */
public interface PatternCaseLabelTree extends CaseLabelTree {

    PatternTree getPattern();
}
