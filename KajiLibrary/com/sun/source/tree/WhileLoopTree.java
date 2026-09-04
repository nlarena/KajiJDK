package com.sun.source.tree;

/**
 * `while (cond) ...`.
 */
public interface WhileLoopTree extends StatementTree {

    ExpressionTree getCondition();

    StatementTree getStatement();
}
