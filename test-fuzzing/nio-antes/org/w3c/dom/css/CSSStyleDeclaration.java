package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * A block of declarations: what goes between the braces of a rule, or the `style=""` of an element.
 *
 * <p>It can be seen in two ways and both are the same: as text (`getCssText`) or as a set of
 * properties. It is **live**: writing a property changes the text and vice versa.
 *
 * <p>Two ways of reading a property, and the difference matters. `getPropertyValue` gives the text
 * and always works; `getPropertyCSSValue` gives the structured value and **returns null for the
 * shorthand properties** --`background`, `font`, `margin`--, because the value of a shorthand is
 * not one value but several. Whoever wants those has to ask for the longhand properties one by one.
 *
 * <p>`item(i)` walks the **names** of the written properties, in the order of the document; it is
 * what allows listing a block without knowing beforehand what it has.
 */
public interface CSSStyleDeclaration {

    /** The whole block as text. */
    String getCssText();

    /**
     * It replaces the whole block.
     *
     * @throws DOMException `SYNTAX_ERR` if it does not parse; `NO_MODIFICATION_ALLOWED_ERR` if the
     *     block is read-only
     */
    void setCssText(String cssText) throws DOMException;

    /** The value of that property as text, or the empty string if it is not written. */
    String getPropertyValue(String propertyName);

    /** The structured value, or null. See the note on shorthands. */
    CSSValue getPropertyCSSValue(String propertyName);

    /**
     * It removes that property and returns the value it had, or the empty string if it was not
     * there.
     *
     * @throws DOMException `NO_MODIFICATION_ALLOWED_ERR` if the block is read-only
     */
    String removeProperty(String propertyName) throws DOMException;

    /** `"important"` if the property carries it, the empty string if not. */
    String getPropertyPriority(String propertyName);

    /**
     * It writes that property. `priority` is `"important"` or the empty string.
     *
     * @throws DOMException `SYNTAX_ERR` if the value does not parse; `NO_MODIFICATION_ALLOWED_ERR`
     *     if the block or that property is read-only
     */
    void setProperty(String propertyName, String value, String priority) throws DOMException;

    /** How many properties are written. */
    int getLength();

    /** The name of the property at that position, or the empty string if the index is not valid. */
    String item(int index);

    /** The rule that contains this block, or null if it is the `style` of an element. */
    CSSRule getParentRule();
}
