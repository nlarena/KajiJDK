package org.w3c.dom.html;

/**
 * Un `<hr>`.
 */
public interface HTMLHRElement extends HTMLElement {

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);

    /** El atributo `noShade`. */
    boolean getNoShade();

    /** Fija el atributo `noShade`. */
    void setNoShade(boolean noShade);

    /** El tamanio visible. */
    String getSize();

    /** Fija el tamanio visible. */
    void setSize(String size);

    /** El ancho. */
    String getWidth();

    /** Fija el ancho. */
    void setWidth(String width);
}
