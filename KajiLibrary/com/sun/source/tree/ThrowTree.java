package com.sun.source.tree;

/**
 * `throw`.
 */
public interface ThrowTree extends StatementTree {

    ExpressionTree getExpression();
}
