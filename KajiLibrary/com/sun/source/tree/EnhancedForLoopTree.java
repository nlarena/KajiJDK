package com.sun.source.tree;

/**
 * El `for` sobre una coleccion.
 */
public interface EnhancedForLoopTree extends StatementTree {

    VariableTree getVariable();

    ExpressionTree getExpression();

    StatementTree getStatement();
}
