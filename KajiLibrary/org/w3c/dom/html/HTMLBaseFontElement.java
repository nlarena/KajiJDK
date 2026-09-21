package org.w3c.dom.html;

/**
 * A `<basefont>`. Obsolete in HTML 4.
 */
public interface HTMLBaseFontElement extends HTMLElement {

    /** The `color` attribute. */
    String getColor();

    /** It sets the `color` attribute. */
    void setColor(String color);

    /** The `face` attribute. */
    String getFace();

    /** It sets the `face` attribute. */
    void setFace(String face);

    /** The visible size. */
    String getSize();

    /** It sets the visible size. */
    void setSize(String size);
}
