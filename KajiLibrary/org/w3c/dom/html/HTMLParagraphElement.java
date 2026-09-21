package org.w3c.dom.html;

/**
 * A `<p>`.
 */
public interface HTMLParagraphElement extends HTMLElement {

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);
}
