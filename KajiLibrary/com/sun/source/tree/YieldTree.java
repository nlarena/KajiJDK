package com.sun.source.tree;

/**
 * `yield`, que devuelve el valor de un {@link SwitchExpressionTree}.
 */
public interface YieldTree extends StatementTree {

    ExpressionTree getValue();
}
