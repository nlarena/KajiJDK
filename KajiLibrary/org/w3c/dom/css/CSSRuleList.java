package org.w3c.dom.css;

/**
 * Las reglas de una hoja o de un `@media`, en el orden del documento.
 *
 * <p>Es **viva**: insertar una regla cambia lo que `getLength` contesta sin volver a pedir la lista.
 */
public interface CSSRuleList {

    /** Cuantas reglas hay. */
    int getLength();

    /** La regla en esa posicion, o nulo si el indice esta fuera de rango. */
    CSSRule item(int index);
}
