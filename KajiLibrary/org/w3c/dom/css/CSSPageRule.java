package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/** A `@page`: the declarations that apply to a printed page. */
public interface CSSPageRule extends CSSRule {

    /** The selector of the page --`:first`, `:left`--, or the empty string. */
    String getSelectorText();

    /**
     * It changes the page selector.
     *
     * @throws DOMException `SYNTAX_ERR` if it does not parse; `NO_MODIFICATION_ALLOWED_ERR` if the
     *     rule is read-only
     */
    void setSelectorText(String selectorText) throws DOMException;

    /** The declarations of the page. */
    CSSStyleDeclaration getStyle();
}
