package com.sun.source.tree;

import java.util.List;

/**
 * A record pattern, such as `Point(int x, int y)`, with its nested
 * patterns.
 */
public interface DeconstructionPatternTree extends PatternTree {

    ExpressionTree getDeconstructor();

    List<? extends PatternTree> getNestedPatterns();
}
