package com.sun.source.tree;

/**
 * El patron que liga un nombre, como el `String s` de `x instanceof String s`.
 */
public interface BindingPatternTree extends PatternTree {

    VariableTree getVariable();
}
