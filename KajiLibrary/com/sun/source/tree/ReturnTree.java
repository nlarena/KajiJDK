package com.sun.source.tree;

/**
 * `return`, con expresion o sin ella.
 */
public interface ReturnTree extends StatementTree {

    ExpressionTree getExpression();
}
