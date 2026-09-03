package org.w3c.dom.html;

/**
 * Un `<title>`.
 */
public interface HTMLTitleElement extends HTMLElement {

    /** El texto que se muestra. */
    String getText();

    /** Fija el texto que se muestra. */
    void setText(String text);
}
