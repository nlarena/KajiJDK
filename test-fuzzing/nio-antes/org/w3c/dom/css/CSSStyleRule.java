package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * A style rule: a selector and the declarations it applies to it.
 *
 * <p>`getSelectorText` gives the selector as text, including the groups separated by commas. The
 * specification does not expose the parsed selector, so whoever needs the parts has to parse it on
 * their own -- it is a limitation of CSS 2, not of this implementation.
 */
public interface CSSStyleRule extends CSSRule {

    /** The selector, as text. */
    String getSelectorText();

    /**
     * It replaces the selector.
     *
     * @throws DOMException `SYNTAX_ERR` if it does not parse; `NO_MODIFICATION_ALLOWED_ERR` if the
     *     rule is read-only
     */
    void setSelectorText(String selectorText) throws DOMException;

    /** The declarations of this rule. The list is live. */
    CSSStyleDeclaration getStyle();
}
