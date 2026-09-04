package com.sun.source.tree;

/**
 * `x instanceof T` y `x instanceof T t`. {@link #getPattern} es `null` en la
 * forma vieja.
 */
public interface InstanceOfTree extends ExpressionTree {

    ExpressionTree getExpression();

    Tree getType();

    /** El patron, o `null` en la forma sin patron. */
    PatternTree getPattern();
}
