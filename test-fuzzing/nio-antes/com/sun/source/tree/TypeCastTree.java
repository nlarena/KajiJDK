package com.sun.source.tree;

/**
 * `(T) x`.
 */
public interface TypeCastTree extends ExpressionTree {

    Tree getType();

    ExpressionTree getExpression();
}
