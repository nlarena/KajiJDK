package org.w3c.dom.html;

/**
 * Un `<td>` o un `<th>`. Las dos son la misma interfaz.
 */
public interface HTMLTableCellElement extends HTMLElement {

    /** La posicion en su fila. */
    int getCellIndex();

    /** El atributo `abbr`. */
    String getAbbr();

    /** Fija el atributo `abbr`. */
    void setAbbr(String abbr);

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);

    /** El atributo `axis`. */
    String getAxis();

    /** Fija el atributo `axis`. */
    void setAxis(String axis);

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

    /** Cuantas columnas ocupa. */
    int getColSpan();

    /** Fija cuantas columnas ocupa. */
    void setColSpan(int colSpan);

    /** El atributo `headers`. */
    String getHeaders();

    /** Fija el atributo `headers`. */
    void setHeaders(String headers);

    /** El alto. */
    String getHeight();

    /** Fija el alto. */
    void setHeight(String height);

    /** El atributo `noWrap`. */
    boolean getNoWrap();

    /** Fija el atributo `noWrap`. */
    void setNoWrap(boolean noWrap);

    /** Cuantas filas ocupa. */
    int getRowSpan();

    /** Fija cuantas filas ocupa. */
    void setRowSpan(int rowSpan);

    /** El atributo `scope`. */
    String getScope();

    /** Fija el atributo `scope`. */
    void setScope(String scope);

    /** El atributo `vAlign`. */
    String getVAlign();

    /** Fija el atributo `vAlign`. */
    void setVAlign(String vAlign);

    /** El ancho. */
    String getWidth();

    /** Fija el ancho. */
    void setWidth(String width);
}
