package com.sun.source.tree;

/**
 * `a += b` y familia. Distinto de {@link AssignmentTree} porque no es
 * azucar exacto: lleva un cast implicito que `a = a + b` no tiene.
 */
public interface CompoundAssignmentTree extends ExpressionTree {

    ExpressionTree getVariable();

    ExpressionTree getExpression();
}
