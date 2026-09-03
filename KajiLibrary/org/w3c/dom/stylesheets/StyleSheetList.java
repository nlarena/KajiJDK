package org.w3c.dom.stylesheets;

/**
 * Las hojas de estilo de un documento, en el orden en que estan declaradas.
 *
 * <p>Es **viva**: agregar un `<link>` al documento cambia lo que `getLength` contesta sin volver a
 * pedir la lista.
 */
public interface StyleSheetList {

    /** Cuantas hojas hay. */
    int getLength();

    /** La hoja en esa posicion, o nulo si el indice esta fuera de rango. */
    StyleSheet item(int index);
}
