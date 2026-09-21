package com.sun.source.tree;

import java.util.List;

/**
 * The use of an annotation, with its arguments.
 */
public interface AnnotationTree extends ExpressionTree {

    Tree getAnnotationType();

    List<? extends ExpressionTree> getArguments();
}
