package com.sun.source.tree;

/**
 * Un bloque `synchronized`.
 */
public interface SynchronizedTree extends StatementTree {

    ExpressionTree getExpression();

    BlockTree getBlock();
}
