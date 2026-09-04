package com.sun.source.tree;

/**
 * Una constante escrita en el fuente. {@link #getValue} devuelve {@code Object}
 * porque el tipo depende de {@link Tree#getKind}, y es {@code null} para el literal `null`.
 */
public interface LiteralTree extends ExpressionTree {

    /** El valor, cuyo tipo depende de {@link Tree#getKind}; `null` para el literal `null`. */
    Object getValue();
}
