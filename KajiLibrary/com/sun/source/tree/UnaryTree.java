package com.sun.source.tree;

/**
 * Cualquier operador de un operando. Cual es lo dice {@link Tree#getKind}: ahi se
 * distinguen `++x` de `x++`, que tienen la misma forma y significados distintos.
 */
public interface UnaryTree extends ExpressionTree {

    ExpressionTree getExpression();
}
