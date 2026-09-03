package org.w3c.dom.css;

/**
 * Un `rect()`, que es lo que lleva la propiedad `clip`.
 *
 * <p>Los cuatro lados son valores primitivos y no numeros porque cada uno puede ser `auto`, que no
 * es una longitud: seria imposible representarlo con un `float`.
 */
public interface Rect {

    /** El lado de arriba. */
    CSSPrimitiveValue getTop();

    /** El lado derecho. */
    CSSPrimitiveValue getRight();

    /** El lado de abajo. */
    CSSPrimitiveValue getBottom();

    /** El lado izquierdo. */
    CSSPrimitiveValue getLeft();
}
