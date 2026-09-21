package com.sun.source.tree;

/**
 * The pattern that binds a name, such as the `String s` of `x instanceof String s`.
 */
public interface BindingPatternTree extends PatternTree {

    VariableTree getVariable();
}
