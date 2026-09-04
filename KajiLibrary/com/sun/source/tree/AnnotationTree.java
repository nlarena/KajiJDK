package com.sun.source.tree;

import java.util.List;

/**
 * El uso de una anotacion, con sus argumentos.
 */
public interface AnnotationTree extends ExpressionTree {

    Tree getAnnotationType();

    List<? extends ExpressionTree> getArguments();
}
