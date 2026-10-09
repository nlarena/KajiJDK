package com.sun.source.tree;

/**
 * `cond ? a : b`.
 */
public interface ConditionalExpressionTree extends ExpressionTree {

    ExpressionTree getCondition();

    ExpressionTree getTrueExpression();

    ExpressionTree getFalseExpression();
}
