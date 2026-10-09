package com.sun.source.tree;

import java.util.List;

/**
 * A lambda.
 *
 * <p>{@link #getBody} returns {@link Tree} and not something more precise because a lambda has
 * two forms -- an expression or a block -- and they share no supertype: {@link ExpressionTree}
 * and {@link StatementTree} are the two halves of the tree. {@link #getBodyKind} is how which
 * of the two came is known, without casting blindly.
 */
public interface LambdaExpressionTree extends ExpressionTree {

    /** Which of the two forms of body the lambda has. */
    enum BodyKind {

        /** `x -> x + 1`. */
        EXPRESSION,
        /** `x -> { return x + 1; }`. */
        STATEMENT
    }

    /** The parameters. Empty in `() -> ...`. */
    List<? extends VariableTree> getParameters();

    /** The body; see {@link #getBodyKind} in order to know what it is. */
    Tree getBody();

    /** Whether the body is an expression or a block. */
    BodyKind getBodyKind();
}
