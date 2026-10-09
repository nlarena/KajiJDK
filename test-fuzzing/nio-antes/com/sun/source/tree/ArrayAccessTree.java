package com.sun.source.tree;

/**
 * `a[i]`.
 */
public interface ArrayAccessTree extends ExpressionTree {

    ExpressionTree getExpression();

    ExpressionTree getIndex();
}
