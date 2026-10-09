package org.w3c.dom.html;

/**
 * A `<title>`.
 */
public interface HTMLTitleElement extends HTMLElement {

    /** The text that is shown. */
    String getText();

    /** It sets the text that is shown. */
    void setText(String text);
}
