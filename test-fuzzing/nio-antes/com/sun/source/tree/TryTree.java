package com.sun.source.tree;

import java.util.List;

/**
 * `try`, with its resources, its `catch`es and its `finally`.
 */
public interface TryTree extends StatementTree {

    BlockTree getBlock();

    List<? extends CatchTree> getCatches();

    BlockTree getFinallyBlock();

    List<? extends Tree> getResources();
}
