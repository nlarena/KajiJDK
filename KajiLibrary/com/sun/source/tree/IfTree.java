package com.sun.source.tree;

/**
 * `if`, con o sin `else`.
 */
public interface IfTree extends StatementTree {

    ExpressionTree getCondition();

    StatementTree getThenStatement();

    StatementTree getElseStatement();
}
