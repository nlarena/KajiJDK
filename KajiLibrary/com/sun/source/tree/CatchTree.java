package com.sun.source.tree;

/**
 * Una clausula `catch`. El parametro es un {@link VariableTree} entero porque puede
 * llevar modificadores y un tipo union (`catch (A | B e)`).
 */
public interface CatchTree extends Tree {

    VariableTree getParameter();

    BlockTree getBlock();
}
