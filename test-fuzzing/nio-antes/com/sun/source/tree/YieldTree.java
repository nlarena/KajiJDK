package com.sun.source.tree;

/**
 * `yield`, which returns the value of a {@link SwitchExpressionTree}.
 */
public interface YieldTree extends StatementTree {

    ExpressionTree getValue();
}
