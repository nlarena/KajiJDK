package com.sun.source.tree;

/**
 * Una expresion usada como sentencia, o sea con un `;` atras.
 */
public interface ExpressionStatementTree extends StatementTree {

    ExpressionTree getExpression();
}
