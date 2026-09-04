package com.sun.source.tree;

/**
 * `assert cond;` o `assert cond : detalle;`.
 */
public interface AssertTree extends StatementTree {

    ExpressionTree getCondition();

    ExpressionTree getDetail();
}
