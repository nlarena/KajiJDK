package com.sun.source.tree;

import java.util.List;

/**
 * `new T[n]` and `{ ... }`. {@link #getDimAnnotations} is a list of lists
 * because each dimension may carry its own type annotations.
 */
public interface NewArrayTree extends ExpressionTree {

    Tree getType();

    List<? extends ExpressionTree> getDimensions();

    List<? extends ExpressionTree> getInitializers();

    List<? extends AnnotationTree> getAnnotations();

    /** Each dimension's annotations, one list per dimension. */
    List<? extends List<? extends AnnotationTree>> getDimAnnotations();
}
