package com.sun.source.tree;

import java.util.List;

/**
 * Un patron de record, como `Punto(int x, int y)`, con sus patrones
 * anidados.
 */
public interface DeconstructionPatternTree extends PatternTree {

    ExpressionTree getDeconstructor();

    List<? extends PatternTree> getNestedPatterns();
}
