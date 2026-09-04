package com.sun.source.tree;

import java.util.List;

/**
 * Una llamada. El metodo viene como {@link #getMethodSelect} —un
 * identificador o un `a.b`— y no como un nombre, porque en el fuente todavia no se resolvio a
 * nada.
 */
public interface MethodInvocationTree extends ExpressionTree {

    List<? extends Tree> getTypeArguments();

    ExpressionTree getMethodSelect();

    List<? extends ExpressionTree> getArguments();
}
