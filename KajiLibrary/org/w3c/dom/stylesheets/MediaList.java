package org.w3c.dom.stylesheets;

import org.w3c.dom.DOMException;

/**
 * Los medios para los que una hoja o una regla aplican: `screen`, `print`, `all`.
 *
 * <p>Es **viva** y ordenada, y se puede ver de dos formas que son la misma: como el texto entero
 * (`getMediaText`) o como una lista indexable. Cambiar cualquiera de las dos cambia la otra.
 *
 * <p>Una lista **vacia** no significa "ningun medio" sino **todos**, que es lo contrario de lo que
 * uno esperaria de una lista vacia. Es lo que dice CSS 2 para una hoja sin atributo `media`.
 */
public interface MediaList {

    /** Los medios como texto, separados por comas. */
    String getMediaText();

    /**
     * Reemplaza la lista entera con ese texto.
     *
     * @throws DOMException `SYNTAX_ERR` si el texto no se puede parsear;
     *     `NO_MODIFICATION_ALLOWED_ERR` si la lista es de solo lectura
     */
    void setMediaText(String mediaText) throws DOMException;

    /** Cuantos medios hay. */
    int getLength();

    /** El medio en esa posicion, o nulo si el indice esta fuera de rango. */
    String item(int index);

    /**
     * Saca ese medio de la lista.
     *
     * @throws DOMException `NOT_FOUND_ERR` si el medio no esta;
     *     `NO_MODIFICATION_ALLOWED_ERR` si la lista es de solo lectura
     */
    void deleteMedium(String oldMedium) throws DOMException;

    /**
     * Agrega ese medio al final. Si ya estaba, se mueve al final en vez de duplicarse.
     *
     * @throws DOMException `INVALID_CHARACTER_ERR` si el medio no es valido;
     *     `NO_MODIFICATION_ALLOWED_ERR` si la lista es de solo lectura
     */
    void appendMedium(String newMedium) throws DOMException;
}
