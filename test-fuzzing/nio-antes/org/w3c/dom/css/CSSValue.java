package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * The value of a CSS declaration.
 *
 * <p>Three forms: a primitive value, a list of values, or `inherit`. The fourth constant,
 * `CSS_CUSTOM`, is for whatever an implementation understands and the DOM does not model -- a
 * shorthand property such as `background`, for example, whose value is neither a primitive nor a
 * list.
 */
public interface CSSValue {

    /** The value is the keyword `inherit`. */
    public static final short CSS_INHERIT = 0;
    /** It is a {@link CSSPrimitiveValue}. */
    public static final short CSS_PRIMITIVE_VALUE = 1;
    /** It is a {@link CSSValueList}. */
    public static final short CSS_VALUE_LIST = 2;
    /** It is something the DOM does not model; see the note of the class. */
    public static final short CSS_CUSTOM = 3;

    /** The value as text. */
    String getCssText();

    /**
     * It replaces the value with that text.
     *
     * @throws DOMException `SYNTAX_ERR` if it does not parse; `INVALID_MODIFICATION_ERR` if the
     *     text describes a value of a form other than the current one;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the value is read-only
     */
    void setCssText(String cssText) throws DOMException;

    /** Which of the four forms it is. */
    short getCssValueType();
}
