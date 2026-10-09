package com.sun.source.tree;

/**
 * `do { ... } while (cond);`.
 */
public interface DoWhileLoopTree extends StatementTree {

    ExpressionTree getCondition();

    StatementTree getStatement();
}
