package com.sun.source.tree;

import java.util.List;

/**
 * Un bloque `{ ... }`. {@link #isStatic} lo distingue de un inicializador estatico,
 * que tiene la misma forma.
 */
public interface BlockTree extends StatementTree {

    /** Si es un inicializador estatico y no un bloque comun. */
    boolean isStatic();

    List<? extends StatementTree> getStatements();
}
