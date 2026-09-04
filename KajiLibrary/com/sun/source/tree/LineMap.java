package com.sun.source.tree;

/**
 * La traduccion entre posiciones absolutas del archivo y pares linea/columna.
 *
 * <p>No es un nodo del arbol: los nodos guardan **posiciones absolutas**, que son un solo numero y
 * no se invalidan si el archivo se reindenta. Traducirlas a linea y columna es caro —hay que saber
 * donde estan todos los saltos de linea— y solo hace falta al mostrarle algo a una persona. De ahi
 * que sea un objeto aparte que se pide una vez por unidad de compilacion.
 */
public interface LineMap {

    long getStartPosition(long a0);

    long getPosition(long a0, long a1);

    long getLineNumber(long a0);

    long getColumnNumber(long a0);
}
