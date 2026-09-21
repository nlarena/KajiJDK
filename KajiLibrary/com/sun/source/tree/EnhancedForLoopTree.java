package com.sun.source.tree;

/**
 * The `for` over a collection.
 */
public interface EnhancedForLoopTree extends StatementTree {

    VariableTree getVariable();

    ExpressionTree getExpression();

    StatementTree getStatement();
}
