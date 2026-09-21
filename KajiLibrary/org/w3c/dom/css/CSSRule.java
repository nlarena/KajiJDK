package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * A rule of a style sheet: the root of the seven forms CSS 2 defines.
 *
 * <p>Which of the seven it is is asked with {@link #getType} and **not** with `instanceof`. Both
 * work, but the numeric type is the one that survives an implementation that does not use the class
 * hierarchy one expects, and it is what the specification defines.
 *
 * <p>`getCssText` returns the whole rule as text, including its selector and its braces. Assigning
 * it replaces the complete rule, it does not add to it: a rule is indivisible from outside.
 */
public interface CSSRule {

    /** A rule this implementation does not recognise. */
    public static final short UNKNOWN_RULE = 0;
    /** A style rule: a selector and its declarations. */
    public static final short STYLE_RULE = 1;
    /** A `@charset`. */
    public static final short CHARSET_RULE = 2;
    /** An `@import`. */
    public static final short IMPORT_RULE = 3;
    /** A `@media`. */
    public static final short MEDIA_RULE = 4;
    /** A `@font-face`. */
    public static final short FONT_FACE_RULE = 5;
    /** A `@page`. */
    public static final short PAGE_RULE = 6;

    /** Which of the seven forms this rule is. */
    short getType();

    /** The whole rule as text. */
    String getCssText();

    /**
     * It replaces the whole rule with that text.
     *
     * @throws DOMException `SYNTAX_ERR` if the text does not parse; `INVALID_MODIFICATION_ERR` if
     *     it describes a rule of a type other than the current one; `NO_MODIFICATION_ALLOWED_ERR`
     *     if the rule is read-only
     */
    void setCssText(String cssText) throws DOMException;

    /** The sheet that contains it, or null. */
    CSSStyleSheet getParentStyleSheet();

    /** The rule that contains it --only a `@media` contains others--, or null. */
    CSSRule getParentRule();
}
