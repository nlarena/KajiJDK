package com.sun.source.tree;

/**
 * Cualquier operador de dos operandos. Cual es lo dice {@link Tree#getKind}, no el
 * tipo: `+` y `*` son los dos un {@code BinaryTree} y se distinguen por {@code PLUS} y
 * {@code MULTIPLY}. Es la razon de que este arbol tenga 117 {@link Tree.Kind} y solo 76 interfaces.
 */
public interface BinaryTree extends ExpressionTree {

    ExpressionTree getLeftOperand();

    ExpressionTree getRightOperand();
}
