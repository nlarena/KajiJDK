package com.sun.source.tree;

/**
 * Any operator of one operand. Which one it is is said by {@link Tree#getKind}: there
 * `++x` and `x++` are told apart, which have the same shape and different meanings.
 */
public interface UnaryTree extends ExpressionTree {

    ExpressionTree getExpression();
}
