package com.sun.source.tree;

import java.util.List;

/**
 * Un tipo con argumentos, como `List<String>`.
 */
public interface ParameterizedTypeTree extends Tree {

    Tree getType();

    List<? extends Tree> getTypeArguments();
}
