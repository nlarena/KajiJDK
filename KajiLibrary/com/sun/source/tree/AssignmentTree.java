package com.sun.source.tree;

/**
 * `a = b`. It is an **expression**, not a statement: in Java an assignment has a
 * value, which is what allows `a = b = c`.
 */
public interface AssignmentTree extends ExpressionTree {

    ExpressionTree getVariable();

    ExpressionTree getExpression();
}
