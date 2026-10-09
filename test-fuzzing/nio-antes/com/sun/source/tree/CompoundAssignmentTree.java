package com.sun.source.tree;

/**
 * `a += b` and family. Different from {@link AssignmentTree} because it is not exact
 * sugar: it carries an implicit cast that `a = a + b` does not have.
 */
public interface CompoundAssignmentTree extends ExpressionTree {

    ExpressionTree getVariable();

    ExpressionTree getExpression();
}
