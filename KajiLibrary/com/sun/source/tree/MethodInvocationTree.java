package com.sun.source.tree;

import java.util.List;

/**
 * A call. The method comes as {@link #getMethodSelect} -- an
 * identifier or an `a.b` -- and not as a name, because in the source it has not been resolved to
 * anything yet.
 */
public interface MethodInvocationTree extends ExpressionTree {

    List<? extends Tree> getTypeArguments();

    ExpressionTree getMethodSelect();

    List<? extends ExpressionTree> getArguments();
}
