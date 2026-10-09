package com.sun.source.tree;

/**
 * `x instanceof T` and `x instanceof T t`. {@link #getPattern} is `null` in the
 * old form.
 */
public interface InstanceOfTree extends ExpressionTree {

    ExpressionTree getExpression();

    Tree getType();

    /** The pattern, or `null` in the form with no pattern. */
    PatternTree getPattern();
}
