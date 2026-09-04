package com.sun.source.tree;

import javax.lang.model.element.Name;

/**
 * Un nombre suelto.
 */
public interface IdentifierTree extends ExpressionTree {

    Name getName();
}
