package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * A CSS value that does not break down: a number with its unit, a colour, a string, a `url()`.
 *
 * <p>The twenty-six constants are the units and the forms CSS 2 defines, and
 * {@link #getPrimitiveType} says which one it is. From there it follows which accessor serves:
 * `getFloatValue` for the ones that are numbers with a unit, `getStringValue` for the textual ones,
 * and the three specific ones for `counter()`, `rect()` and a colour.
 *
 * <p><strong>`getFloatValue` converts.</strong> Asking for `CSS_MM` of a value kept in `CSS_CM`
 * gives the converted number, not an error, and that is what makes the method useful. But it only
 * converts **within the same family** --lengths with lengths, angles with angles, times with
 * times--: asking an angle for centimetres is `INVALID_ACCESS_ERR`. Careful with the relative
 * lengths: `em` and `ex` depend on the font, so they are not converted to absolute ones.
 */
public interface CSSPrimitiveValue extends CSSValue {

    /** The unit is not known. */
    public static final short CSS_UNKNOWN = 0;
    /** A number with no unit. */
    public static final short CSS_NUMBER = 1;
    /** A percentage. */
    public static final short CSS_PERCENTAGE = 2;
    /** `em`: the size of the current font. */
    public static final short CSS_EMS = 3;
    /** `ex`: the height of the x of the current font. */
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
    /** A number with a unit this implementation does not recognise. */
    public static final short CSS_DIMENSION = 18;
    /** A string in quotes. */
    public static final short CSS_STRING = 19;
    /** A `url()`. */
    public static final short CSS_URI = 20;
    /** An identifier, such as `auto` or `red`. */
    public static final short CSS_IDENT = 21;
    /** An `attr()`. */
    public static final short CSS_ATTR = 22;
    /** A `counter()` or `counters()`. */
    public static final short CSS_COUNTER = 23;
    /** A `rect()`. */
    public static final short CSS_RECT = 24;
    /** A colour. */
    public static final short CSS_RGBCOLOR = 25;

    /** Which of the twenty-six forms it is. */
    short getPrimitiveType();

    /**
     * It sets the value as a number with that unit.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` if the unit does not serve for this value;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the value is read-only
     */
    void setFloatValue(short unitType, float floatValue) throws DOMException;

    /**
     * The numeric value in that unit, **converting** if need be. See the note of the class.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` if this value is not numeric, or if the unit asked
     *     for is of a family other than its own
     */
    float getFloatValue(short unitType) throws DOMException;

    /**
     * It sets the value as text of that form.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` if the form is not textual;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the value is read-only
     */
    void setStringValue(short stringType, String stringValue) throws DOMException;

    /**
     * The value as text, without the quotes or the `url(...)` around it.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` if this value is not textual
     */
    String getStringValue() throws DOMException;

    /**
     * The value as a counter.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` if it is not a `counter()`
     */
    Counter getCounterValue() throws DOMException;

    /**
     * The value as a rectangle.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` if it is not a `rect()`
     */
    Rect getRectValue() throws DOMException;

    /**
     * The value as a colour.
     *
     * @throws DOMException `INVALID_ACCESS_ERR` if it is not a colour
     */
    RGBColor getRGBColorValue() throws DOMException;
}
