package com.sun.source.tree;

/**
 * A `catch` clause. The parameter comes as a whole {@link VariableTree} because it may
 * carry modifiers and a union type (`catch (A | B e)`).
 */
public interface CatchTree extends Tree {

    VariableTree getParameter();

    BlockTree getBlock();
}
