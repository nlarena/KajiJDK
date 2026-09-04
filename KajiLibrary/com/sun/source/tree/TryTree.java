package com.sun.source.tree;

import java.util.List;

/**
 * `try`, con sus recursos, sus `catch` y su `finally`.
 */
public interface TryTree extends StatementTree {

    BlockTree getBlock();

    List<? extends CatchTree> getCatches();

    BlockTree getFinallyBlock();

    List<? extends Tree> getResources();
}
