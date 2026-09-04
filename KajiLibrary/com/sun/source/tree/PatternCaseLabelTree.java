package com.sun.source.tree;

/**
 * Una etiqueta de `case` que es un patron.
 */
public interface PatternCaseLabelTree extends CaseLabelTree {

    PatternTree getPattern();
}
