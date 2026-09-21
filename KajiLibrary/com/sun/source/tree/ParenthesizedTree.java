package com.sun.source.tree;

/**
 * An expression between parentheses.
 *
 * <p>It is kept in the tree even though it does not change the meaning, and it is not redundant:
 * a tool that rewrites code has to be able to re-emit the parentheses the author put, and one
 * that analyses style may want to see them.
 */
public interface ParenthesizedTree extends ExpressionTree {

    ExpressionTree getExpression();
}
