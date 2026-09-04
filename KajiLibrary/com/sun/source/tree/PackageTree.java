package com.sun.source.tree;

import java.util.List;

/**
 * La declaracion `package`, con sus anotaciones.
 */
public interface PackageTree extends Tree {

    List<? extends AnnotationTree> getAnnotations();

    ExpressionTree getPackageName();
}
