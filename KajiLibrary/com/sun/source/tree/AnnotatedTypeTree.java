package com.sun.source.tree;

import java.util.List;

/**
 * Un tipo con anotaciones de tipo pegadas, como `@NonNull String`.
 */
public interface AnnotatedTypeTree extends ExpressionTree {

    List<? extends AnnotationTree> getAnnotations();

    ExpressionTree getUnderlyingType();
}
