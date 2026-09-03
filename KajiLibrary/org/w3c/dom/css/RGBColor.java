package org.w3c.dom.css;

/**
 * Un color, por sus tres componentes.
 *
 * <p>Cada componente es un {@link CSSPrimitiveValue} y no un entero porque CSS admite las dos
 * formas --`rgb(255,0,0)` y `rgb(100%,0%,0%)`--, y el tipo primitivo de cada componente dice cual
 * de las dos se escribio. Reducirlas a un `int` perderia esa distincion al reescribir la hoja.
 */
public interface RGBColor {

    /** El rojo. */
    CSSPrimitiveValue getRed();

    /** El verde. */
    CSSPrimitiveValue getGreen();

    /** El azul. */
    CSSPrimitiveValue getBlue();
}
