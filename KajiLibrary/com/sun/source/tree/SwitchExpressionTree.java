package com.sun.source.tree;

import java.util.List;

/**
 * Un `switch` usado como expresion, o sea el que produce un valor con
 * `yield` o con `->`.
 */
public interface SwitchExpressionTree extends ExpressionTree {

    ExpressionTree getExpression();

    List<? extends CaseTree> getCases();
}
