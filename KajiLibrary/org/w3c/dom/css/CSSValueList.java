package org.w3c.dom.css;

/**
 * Un valor que es una secuencia de valores, como el `font-family` de tres nombres.
 *
 * <p>Es **viva**: si el valor cambia, la lista lo refleja sin volver a pedirla.
 */
public interface CSSValueList extends CSSValue {

    /** Cuantos valores hay. */
    int getLength();

    /** El valor en esa posicion, o nulo si el indice esta fuera de rango. */
    CSSValue item(int index);
}
