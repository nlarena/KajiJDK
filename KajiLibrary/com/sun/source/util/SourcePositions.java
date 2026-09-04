package com.sun.source.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

/**
 * Donde empieza y termina un nodo dentro del archivo.
 *
 * <h2>Por que no vive en el nodo</h2>
 *
 * <p>Porque una posicion solo significa algo <strong>dentro de una unidad de compilacion</strong>, y
 * un nodo no sabe en cual esta: los arboles se pueden compartir y reusar. De ahi que los dos metodos
 * pidan el {@link CompilationUnitTree} y no sean getters del nodo.
 *
 * <p>Las posiciones son absolutas —un solo numero desde el principio del archivo— y no linea y
 * columna. Convertirlas es trabajo de {@link com.sun.source.tree.LineMap}, y se hace solo al
 * mostrarle algo a una persona.
 */
public interface SourcePositions {

    /**
     * Donde empieza, o {@link javax.tools.Diagnostic#NOPOS} si el nodo no vino del fuente — un nodo
     * sintetico que agrego el compilador no esta escrito en ningun lado.
     */
    long getStartPosition(CompilationUnitTree file, Tree tree);

    /** Donde termina, o {@code NOPOS}. */
    long getEndPosition(CompilationUnitTree file, Tree tree);
}
