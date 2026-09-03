package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.CharacterData -- lo que comparten los nodos que son una tira de
 * caracteres: `Text`, `Comment` y `CDATASection`.
 *
 * <p>Lo que agrega sobre `Node` son las operaciones de edicion sobre esa tira --insertar, borrar,
 * reemplazar por rango-- que existen para no tener que leer el texto entero, modificarlo en Java y
 * volver a escribirlo. En un nodo de varios megas la diferencia se nota.
 *
 * <p><strong>Los offsets son en unidades de 16 bits, no en puntos de codigo.</strong> `getLength()`
 * cuenta lo mismo que `String.length()`, asi que un caracter fuera del plano basico cuenta dos y un
 * `deleteData` mal calculado puede partir un par sustituto por el medio. La norma lo hereda de
 * `DOMString`, que se definio como UTF-16 y no como texto.
 */
public interface CharacterData extends Node {

    String getData() throws DOMException;

    void setData(String data) throws DOMException;

    /** En unidades de 16 bits. */
    int getLength();

    /**
     * @throws DOMException con `INDEX_SIZE_ERR` si `offset` esta fuera de rango o `count` es
     *         negativo. Que `offset + count` pase del final **no** es error: se recorta.
     */
    String substringData(int offset, int count) throws DOMException;

    void appendData(String arg) throws DOMException;

    void insertData(int offset, String arg) throws DOMException;

    void deleteData(int offset, int count) throws DOMException;

    /** No equivale a `deleteData` mas `insertData`: es una sola operacion y una sola mutacion. */
    void replaceData(int offset, int count, String arg) throws DOMException;
}
