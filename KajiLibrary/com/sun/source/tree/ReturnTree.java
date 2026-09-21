package com.sun.source.tree;

/**
 * `return`, with an expression or without one.
 */
public interface ReturnTree extends StatementTree {

    ExpressionTree getExpression();
}
