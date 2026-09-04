package com.sun.source.tree;

/**
 * Una etiqueta de `case` que es una constante, o sea el `case` clasico.
 */
public interface ConstantCaseLabelTree extends CaseLabelTree {

    ExpressionTree getConstantExpression();
}
