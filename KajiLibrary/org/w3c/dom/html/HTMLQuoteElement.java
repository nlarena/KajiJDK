package org.w3c.dom.html;

/**
 * Un `<blockquote>` o un `<q>`.
 */
public interface HTMLQuoteElement extends HTMLElement {

    /** El atributo `cite`. */
    String getCite();

    /** Fija el atributo `cite`. */
    void setCite(String cite);
}
