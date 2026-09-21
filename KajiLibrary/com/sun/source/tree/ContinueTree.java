package com.sun.source.tree;

import javax.lang.model.element.Name;

/**
 * `continue`, with a label or without one.
 */
public interface ContinueTree extends StatementTree {

    Name getLabel();
}
