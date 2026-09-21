package com.sun.source.tree;

/**
 * An `import`. {@link #isStatic} and {@link #isModule} tell the three forms apart.
 */
public interface ImportTree extends Tree {

    /** Whether it is an `import static`. */
    boolean isStatic();

    /** Whether it is an `import module`. */
    boolean isModule();

    Tree getQualifiedIdentifier();
}
