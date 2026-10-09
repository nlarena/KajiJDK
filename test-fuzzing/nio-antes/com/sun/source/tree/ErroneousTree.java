package com.sun.source.tree;

import java.util.List;

/**
 * Code that could not be parsed.
 *
 * <p>It extends {@link ExpressionTree} and keeps in {@link #getErrorTrees} what was understood.
 * It is what allows an IDE to go on giving autocompletion over a half-written file: the tree
 * represents the error instead of not existing.
 */
public interface ErroneousTree extends ExpressionTree {

    List<? extends Tree> getErrorTrees();
}
