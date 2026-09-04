package com.sun.source.tree;

import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;

/**
 * Los modificadores y las anotaciones de una declaracion.
 */
public interface ModifiersTree extends Tree {

    Set<Modifier> getFlags();

    List<? extends AnnotationTree> getAnnotations();
}
