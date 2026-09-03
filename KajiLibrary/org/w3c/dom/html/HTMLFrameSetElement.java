package org.w3c.dom.html;

/**
 * Un `<frameset>`.
 */
public interface HTMLFrameSetElement extends HTMLElement {

    /** El atributo `cols`. */
    String getCols();

    /** Fija el atributo `cols`. */
    void setCols(String cols);

    /** Las filas, en una coleccion viva. */
    String getRows();

    /** Fija las filas, en una coleccion viva. */
    void setRows(String rows);
}
