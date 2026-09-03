package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * Un valor CSS que no se descompone: un numero con su unidad, un color, una cadena, un `url()`.
 *
 * <p>Las veintiseis constantes son las unidades y las formas que CSS 2 define, y {@link
 * #getPrimitiveType} dice cual es. De ahi se deduce que accesor sirve: `getFloatValue` para los que
 * son numeros con unidad, `getStringValue` para los textuales, y los tres especificos para
 * `counter()`, `rect()` y un color.
 *
 * <p><strong>`getFloatValue` convierte.</strong> Pedir `CSS_MM` a un valor guardado en `CSS_CM` da
 * el numero convertido, no un error, y eso es lo que hace util al metodo. Pero solo convierte
 * **dentro de la misma familia** --longitudes con longitudes, angulos con angulos, tiempos con
 * tiempos--: pedirle centimetros a un angulo es `INVALID_ACCESS_ERR`. Ojo con las longitudes
 * relativas: `em` y `ex` dependen de la fuente, asi que no se convierten a absolutas.
 */
public interface CSSPrimitiveValue extends CSSValue {

    /** La unidad no se conoce. */
    public static final short CSS_UNKNOWN = 0;
    /** Un numero sin unidad. */
    public static final short CSS_NUMBER = 1;
    /** Un porcentaje. */
    public static final short CSS_PERCENTAGE = 2;
    /** `em`: el tamano de la fuente actual. */
    public static final short CSS_EMS = 3;
    /** `ex`: la altura de la x de la fuente actual. */
    public static final short CSS_EXS = 4;
    /** `px`. */
    public static final short CSS_PX = 5;
    /** `cm`. */
    public static final short CSS_CM = 6;
    /** `mm`. */
    public static final short CSS_MM = 7;
    /** `in`. */
    public static final short CSS_IN = 8;
    /** `pt`. */
    public static final short CSS_PT = 9;
    /** `pc`. */
    public static final short CSS_PC = 10;
    /** `deg`. */
    public static final short CSS_DEG = 11;
    /** `rad`. */
    public static final short CSS_RAD = 12;
    /** `grad`. */
    public static final short CSS_GRAD = 13;
    /** `ms`. */
    public static final short CSS_MS = 14;
    /** `s`. */
    public static final short CSS_S = 15;
    /** `Hz`. */
    public static final short CSS_HZ = 16;
    /** `kHz`. */
    public static final short CSS_KHZ = 17;
    /** Un numero con una unidad que esta implementacion no reconoce. */
    public static final short CSS_DIMENSION = 18;
    /** Una cadena entre comillas. */
    public static final short CSS_STRING = 19;
    /** Un `url()`. */
    public static final short CSS_URI = 20;
    /** Un identificador, como `auto` o `red`. */
    public static final short CSS_IDENT = 21;
    /** Un `attr()`. */
    public static final short CSS_ATTR = 22;
    /** Un `counter()` o `counters()`. */
    public static final short CSS_COUNTER = 23;
    /** Un `rect()`. */
    public static final short CSS_RECT = 24;
    /** Un color. */
    public static final short CSS_RGBCOLOR = 25;

    /** Cual de las veintiseis formas es. */
    short getPrimitiveType();

    /**
     * Fija el valor como un numero con esa unidad.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` si la unidad no sirve para este valor;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el valor es de solo lectura
     */
    void setFloatValue(short unitType, float floatValue) throws DOMException;

    /**
     * El valor numerico en esa unidad, **convirtiendo** si hace falta. Ver la nota de la clase.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` si este valor no es numerico, o si la unidad
     *     pedida es de otra familia que la suya
     */
    float getFloatValue(short unitType) throws DOMException;

    /**
     * Fija el valor como texto de esa forma.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` si la forma no es textual;
     *     `NO_MODIFICATION_ALLOWED_ERR` si el valor es de solo lectura
     */
    void setStringValue(short stringType, String stringValue) throws DOMException;

    /**
     * El valor como texto, sin las comillas ni el `url(...)` de alrededor.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` si este valor no es textual
     */
    String getStringValue() throws DOMException;

    /**
     * El valor como contador.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` si no es un `counter()`
     */
    Counter getCounterValue() throws DOMException;

    /**
     * El valor como rectangulo.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` si no es un `rect()`
     */
    Rect getRectValue() throws DOMException;

    /**
     * El valor como color.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` si no es un color
     */
    RGBColor getRGBColorValue() throws DOMException;
}
