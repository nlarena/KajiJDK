package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;

/**
 * A type parameter with its bounds, such as the `<T extends Number>` of a
 * declaration.
 */
public interface TypeParameterTree extends Tree {

    Name getName();

    List<? extends Tree> getBounds();

    List<? extends AnnotationTree> getAnnotations();
}
