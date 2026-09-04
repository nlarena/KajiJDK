package com.sun.source.tree;

/**
 * Un `import`. {@link #isStatic} y {@link #isModule} distinguen las tres formas.
 */
public interface ImportTree extends Tree {

    /** Si es un `import static`. */
    boolean isStatic();

    /** Si es un `import module`. */
    boolean isModule();

    Tree getQualifiedIdentifier();
}
