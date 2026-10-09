package org.w3c.dom.html;

/**
 * A `<blockquote>` or a `<q>`.
 */
public interface HTMLQuoteElement extends HTMLElement {

    /** The `cite` attribute. */
    String getCite();

    /** It sets the `cite` attribute. */
    void setCite(String cite);
}
