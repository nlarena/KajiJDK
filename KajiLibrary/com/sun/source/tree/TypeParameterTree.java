package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;

/**
 * Un parametro de tipo con sus limites, como el `<T extends Number>` de una
 * declaracion.
 */
public interface TypeParameterTree extends Tree {

    Name getName();

    List<? extends Tree> getBounds();

    List<? extends AnnotationTree> getAnnotations();
}
