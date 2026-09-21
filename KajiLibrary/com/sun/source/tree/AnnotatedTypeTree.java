package com.sun.source.tree;

import java.util.List;

/**
 * A type with type annotations stuck onto it, such as `@NonNull String`.
 */
public interface AnnotatedTypeTree extends ExpressionTree {

    List<? extends AnnotationTree> getAnnotations();

    ExpressionTree getUnderlyingType();
}
