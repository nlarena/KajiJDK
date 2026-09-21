package com.sun.source.tree;

import java.util.List;

/**
 * The `package` declaration, with its annotations.
 */
public interface PackageTree extends Tree {

    List<? extends AnnotationTree> getAnnotations();

    ExpressionTree getPackageName();
}
