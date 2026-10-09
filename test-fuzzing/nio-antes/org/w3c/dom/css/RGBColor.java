package org.w3c.dom.css;

/**
 * A colour, by its three components.
 *
 * <p>Each component is a {@link CSSPrimitiveValue} and not an integer because CSS admits both forms
 * --`rgb(255,0,0)` and `rgb(100%,0%,0%)`--, and the primitive type of each component says which of
 * the two was written. Reducing them to an `int` would lose that distinction when rewriting the
 * sheet.
 */
public interface RGBColor {

    /** The red. */
    CSSPrimitiveValue getRed();

    /** The green. */
    CSSPrimitiveValue getGreen();

    /** The blue. */
    CSSPrimitiveValue getBlue();
}
