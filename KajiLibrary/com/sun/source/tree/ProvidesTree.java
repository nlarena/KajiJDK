package com.sun.source.tree;

import java.util.List;

/**
 * A module's `provides ... with ...` directive.
 */
public interface ProvidesTree extends DirectiveTree {

    ExpressionTree getServiceName();

    List<? extends ExpressionTree> getImplementationNames();
}
