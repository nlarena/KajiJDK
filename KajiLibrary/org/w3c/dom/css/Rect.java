package org.w3c.dom.css;

/**
 * A `rect()`, which is what the `clip` property carries.
 *
 * <p>The four sides are primitive values and not numbers because each one may be `auto`, which is
 * not a length: it would be impossible to represent it with a `float`.
 */
public interface Rect {

    /** The top side. */
    CSSPrimitiveValue getTop();

    /** The right side. */
    CSSPrimitiveValue getRight();

    /** The bottom side. */
    CSSPrimitiveValue getBottom();

    /** The left side. */
    CSSPrimitiveValue getLeft();
}
