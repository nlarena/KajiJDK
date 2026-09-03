package org.w3c.dom.html;

import org.w3c.dom.DOMException;

/**
 * Un `<tr>`.
 *
 * <p>`insertCell(-1)` agrega al final, igual que `insertRow` en la tabla. `getRowIndex` es la
 * posicion en la tabla entera y `getSectionRowIndex` la posicion dentro de su seccion: en una tabla
 * con cabecera, la primera fila del cuerpo tiene los dos numeros distintos.
 */
public interface HTMLTableRowElement extends HTMLElement {

    /** La posicion en la tabla. */
    int getRowIndex();

    /** La posicion dentro de su seccion. */
    int getSectionRowIndex();

    /** Las celdas, en una coleccion viva. */
    HTMLCollection getCells();

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);

    /** El atributo `bgColor`. */
    String getBgColor();

    /** Fija el atributo `bgColor`. */
    void setBgColor(String bgColor);

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

    /**
     * Inserta una celda en esa posicion; -1 agrega al final.
     *
     * @throws DOMException `INDEX_SIZE_ERR` si el indice esta fuera de rango
     */
    HTMLElement insertCell(int index) throws org.w3c.dom.DOMException;

    /**
     * Borra la celda de esa posicion; -1 borra la ultima.
     *
     * @throws DOMException `INDEX_SIZE_ERR` si el indice esta fuera de rango
     */
    void deleteCell(int index) throws org.w3c.dom.DOMException;
}
