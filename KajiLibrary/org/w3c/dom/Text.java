package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Text -- el texto suelto entre etiquetas.
 *
 * <p>Lo que agrega sobre `CharacterData` es la nocion de **texto logico**: un mismo parrafo del
 * documento puede estar partido en varios nodos `Text` y `CDATASection` adyacentes --pasa cuando en
 * el medio habia una referencia a entidad que se expandio-- y `getWholeText()` devuelve la
 * concatenacion de todos ellos, que es lo que el autor del XML escribio. `replaceWholeText` hace la
 * operacion inversa: reemplaza el grupo entero por un solo nodo.
 */
public interface Text extends CharacterData {

    /**
     * Parte este nodo en dos hermanos y devuelve el **segundo**, el que se queda con lo de `offset`
     * en adelante.
     *
     * @throws DOMException con `INDEX_SIZE_ERR` si el offset esta fuera de rango.
     */
    Text splitText(int offset) throws DOMException;

    /**
     * Si este nodo es blanco **ignorable**, o sea sangria que la DTD dice que no es contenido.
     *
     * <p>Sin DTD ni esquema no hay manera de saberlo, y entonces devuelve `false`: no es lo mismo
     * "no es ignorable" que "no se sabe", pero la interfaz solo tiene un `boolean` para decirlo.
     */
    boolean isElementContentWhitespace();

    /** El texto de este nodo mas el de sus hermanos de texto adyacentes. */
    String getWholeText();

    /** Devuelve el nodo que quedo: este, uno nuevo, o `null` si el contenido era vacio. */
    Text replaceWholeText(String content) throws DOMException;
}
