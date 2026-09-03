package org.w3c.dom.html;

import org.w3c.dom.DOMException;

/**
 * Un `<thead>`, `<tbody>` o `<tfoot>`. Las tres son la misma interfaz.
 */
public interface HTMLTableSectionElement extends HTMLElement {

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

    /** El atributo `vAlign`. */
    String getVAlign();

    /** Fija el atributo `vAlign`. */
    void setVAlign(String vAlign);

    /** Las filas, en una coleccion viva. */
    HTMLCollection getRows();

    /**
     * Inserta una fila en esa posicion; -1 agrega al final.
     *
     * @throws DOMException `INDEX_SIZE_ERR` si el indice esta fuera de rango
     */
    HTMLElement insertRow(int index) throws org.w3c.dom.DOMException;

    /**
     * Borra la fila de esa posicion; -1 borra la ultima.
     *
     * @throws DOMException `INDEX_SIZE_ERR` si el indice esta fuera de rango
     */
    void deleteRow(int index) throws org.w3c.dom.DOMException;
}
