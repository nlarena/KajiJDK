package com.sun.source.tree;

import java.util.List;

/**
 * `new T(...)`, con su clase anonima opcional en {@link #getClassBody}.
 */
public interface NewClassTree extends ExpressionTree {

    ExpressionTree getEnclosingExpression();

    List<? extends Tree> getTypeArguments();

    ExpressionTree getIdentifier();

    List<? extends ExpressionTree> getArguments();

    ClassTree getClassBody();
}
