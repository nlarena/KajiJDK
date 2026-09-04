package com.sun.source.tree;

import java.util.List;

/**
 * Una lambda.
 *
 * <p>{@link #getBody} devuelve {@link Tree} y no algo mas preciso porque una lambda tiene dos
 * formas —una expresion o un bloque— y no comparten supertipo: {@link ExpressionTree} y
 * {@link StatementTree} son las dos mitades del arbol. {@link #getBodyKind} es como se sabe cual de
 * las dos vino, sin castear a ciegas.
 */
public interface LambdaExpressionTree extends ExpressionTree {

    /** Cual de las dos formas de cuerpo tiene la lambda. */
    enum BodyKind {

        /** `x -> x + 1`. */
        EXPRESSION,
        /** `x -> { return x + 1; }`. */
        STATEMENT
    }

    /** Los parametros. Vacia en `() -> ...`. */
    List<? extends VariableTree> getParameters();

    /** El cuerpo; ver {@link #getBodyKind} para saber que es. */
    Tree getBody();

    /** Si el cuerpo es una expresion o un bloque. */
    BodyKind getBodyKind();
}
