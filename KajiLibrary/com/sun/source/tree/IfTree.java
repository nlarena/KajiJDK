package com.sun.source.tree;

/**
 * `if`, with or without `else`.
 */
public interface IfTree extends StatementTree {

    ExpressionTree getCondition();

    StatementTree getThenStatement();

    StatementTree getElseStatement();
}
