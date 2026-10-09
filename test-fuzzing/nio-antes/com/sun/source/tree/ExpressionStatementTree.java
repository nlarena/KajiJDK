package com.sun.source.tree;

/**
 * An expression used as a statement, that is, with a `;` behind it.
 */
public interface ExpressionStatementTree extends StatementTree {

    ExpressionTree getExpression();
}
