package org.w3c.dom.html;

/**
 * Un `<col>` o un `<colgroup>`.
 */
public interface HTMLTableColElement extends HTMLElement {

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);

    /** El atributo `ch`. */
    String getCh();

    /** Fija el atributo `ch`. */
    void setCh(String ch);

    /** El atributo `chOff`. */
    String getChOff();

    /** Fija el atributo `chOff`. */
    void setChOff(String chOff);

    /** El atributo `span`. */
    int getSpan();

    /** Fija el atributo `span`. */
    void setSpan(int span);

    /** El atributo `vAlign`. */
    String getVAlign();

    /** Fija el atributo `vAlign`. */
    void setVAlign(String vAlign);

    /** El ancho. */
    String getWidth();

    /** Fija el ancho. */
    void setWidth(String width);
}
