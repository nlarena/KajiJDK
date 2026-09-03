package org.w3c.dom.html;

/**
 * Un `<basefont>`. Obsoleto en HTML 4.
 */
public interface HTMLBaseFontElement extends HTMLElement {

    /** El atributo `color`. */
    String getColor();

    /** Fija el atributo `color`. */
    void setColor(String color);

    /** El atributo `face`. */
    String getFace();

    /** Fija el atributo `face`. */
    void setFace(String face);

    /** El tamanio visible. */
    String getSize();

    /** Fija el tamanio visible. */
    void setSize(String size);
}
