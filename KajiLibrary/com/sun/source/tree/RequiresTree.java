package com.sun.source.tree;

/**
 * La directiva `requires` de un modulo, con sus dos banderas.
 */
public interface RequiresTree extends DirectiveTree {

    /** Si el modulo hace falta para compilar pero no para correr. */
    boolean isStatic();

    /** Si quien requiera este modulo tambien lee al requerido. */
    boolean isTransitive();

    ExpressionTree getModuleName();
}
