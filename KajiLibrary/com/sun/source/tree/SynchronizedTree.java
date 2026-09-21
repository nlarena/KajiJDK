package com.sun.source.tree;

/**
 * A `synchronized` block.
 */
public interface SynchronizedTree extends StatementTree {

    ExpressionTree getExpression();

    BlockTree getBlock();
}
