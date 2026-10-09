package com.sun.source.tree;

import javax.lang.model.element.Name;

/**
 * `a.b`.
 */
public interface MemberSelectTree extends ExpressionTree {

    ExpressionTree getExpression();

    Name getIdentifier();
}
