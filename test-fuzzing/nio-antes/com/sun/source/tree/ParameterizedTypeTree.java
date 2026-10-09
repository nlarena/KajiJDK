package com.sun.source.tree;

import java.util.List;

/**
 * A type with arguments, such as `List<String>`.
 */
public interface ParameterizedTypeTree extends Tree {

    Tree getType();

    List<? extends Tree> getTypeArguments();
}
