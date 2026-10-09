package com.sun.source.tree;

/**
 * A `case` label that is a constant, that is, the classic `case`.
 */
public interface ConstantCaseLabelTree extends CaseLabelTree {

    ExpressionTree getConstantExpression();
}
