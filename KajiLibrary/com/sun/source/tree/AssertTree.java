package com.sun.source.tree;

/**
 * `assert cond;` or `assert cond : detail;`.
 */
public interface AssertTree extends StatementTree {

    ExpressionTree getCondition();

    ExpressionTree getDetail();
}
