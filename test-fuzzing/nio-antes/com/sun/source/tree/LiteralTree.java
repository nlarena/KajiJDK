package com.sun.source.tree;

/**
 * A constant written in the source. {@link #getValue} returns {@code Object}
 * because the type depends on {@link Tree#getKind}, and it is {@code null} for the `null`
 * literal.
 */
public interface LiteralTree extends ExpressionTree {

    /** The value, whose type depends on {@link Tree#getKind}; `null` for the `null` literal. */
    Object getValue();
}
