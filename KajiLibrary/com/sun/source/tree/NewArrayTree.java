package com.sun.source.tree;

import java.util.List;

/**
 * `new T[n]` y `{ ... }`. {@link #getDimAnnotations} es una lista de listas
 * porque cada dimension puede llevar sus propias anotaciones de tipo.
 */
public interface NewArrayTree extends ExpressionTree {

    Tree getType();

    List<? extends ExpressionTree> getDimensions();

    List<? extends ExpressionTree> getInitializers();

    List<? extends AnnotationTree> getAnnotations();

    /** Las anotaciones de cada dimension, una lista por dimension. */
    List<? extends List<? extends AnnotationTree>> getDimAnnotations();
}
