package com.sun.source.tree;

import javax.lang.model.element.Name;

/**
 * A loose name.
 */
public interface IdentifierTree extends ExpressionTree {

    Name getName();
}
