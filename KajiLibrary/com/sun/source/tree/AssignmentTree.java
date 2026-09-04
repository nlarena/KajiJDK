package com.sun.source.tree;

/**
 * `a = b`. Es una **expresion**, no una sentencia: en Java una asignacion tiene
 * valor, que es lo que permite `a = b = c`.
 */
public interface AssignmentTree extends ExpressionTree {

    ExpressionTree getVariable();

    ExpressionTree getExpression();
}
