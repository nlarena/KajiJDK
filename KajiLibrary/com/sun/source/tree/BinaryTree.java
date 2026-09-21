package com.sun.source.tree;

/**
 * Any operator of two operands. Which one it is is said by {@link Tree#getKind}, not
 * the type: `+` and `*` are both a {@code BinaryTree} and are told apart by {@code PLUS} and
 * {@code MULTIPLY}. It is the reason this tree has 117 {@link Tree.Kind} and only 76 interfaces.
 */
public interface BinaryTree extends ExpressionTree {

    ExpressionTree getLeftOperand();

    ExpressionTree getRightOperand();
}
