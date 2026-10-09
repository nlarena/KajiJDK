package com.sun.source.tree;

import javax.lang.model.element.Name;

/**
 * `break`, with a label or without one.
 */
public interface BreakTree extends StatementTree {

    Name getLabel();
}
