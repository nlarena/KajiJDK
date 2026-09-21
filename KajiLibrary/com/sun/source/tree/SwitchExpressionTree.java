package com.sun.source.tree;

import java.util.List;

/**
 * A `switch` used as an expression, that is, the one that produces a value with
 * `yield` or with `->`.
 */
public interface SwitchExpressionTree extends ExpressionTree {

    ExpressionTree getExpression();

    List<? extends CaseTree> getCases();
}
