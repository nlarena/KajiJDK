package org.w3c.dom.css;

/**
 * A value that is a sequence of values, such as the `font-family` with three names.
 *
 * <p>It is **live**: if the value changes, the list reflects it without asking for it again.
 */
public interface CSSValueList extends CSSValue {

    /** How many values there are. */
    int getLength();

    /** The value at that position, or null if the index is out of range. */
    CSSValue item(int index);
}
