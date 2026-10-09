package org.w3c.dom.html;

import org.w3c.dom.DOMException;

/**
 * A `<tr>`.
 *
 * <p>`insertCell(-1)` appends at the end, just like `insertRow` on the table. `getRowIndex` is the
 * position in the whole table and `getSectionRowIndex` the position inside its section: in a table
 * with a header, the first row of the body has the two numbers different.
 */
public interface HTMLTableRowElement extends HTMLElement {

    /** The position in the table. */
    int getRowIndex();

    /** The position inside its section. */
    int getSectionRowIndex();

    /** The cells, in a live collection. */
    HTMLCollection getCells();

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);

    /** The `bgColor` attribute. */
    String getBgColor();

    /** It sets the `bgColor` attribute. */
    void setBgColor(String bgColor);

    /** The `ch` attribute. */
    String getCh();

    /** It sets the `ch` attribute. */
    void setCh(String ch);

    /** The `chOff` attribute. */
    String getChOff();

    /** It sets the `chOff` attribute. */
    void setChOff(String chOff);

    /** The `vAlign` attribute. */
    String getVAlign();

    /** It sets the `vAlign` attribute. */
    void setVAlign(String vAlign);

    /**
     * It inserts a cell at that position; -1 appends at the end.
     *
     * @throws DOMException `INDEX_SIZE_ERR` if the index is out of range
     */
    HTMLElement insertCell(int index) throws org.w3c.dom.DOMException;

    /**
     * It deletes the cell at that position; -1 deletes the last one.
     *
     * @throws DOMException `INDEX_SIZE_ERR` if the index is out of range
     */
    void deleteCell(int index) throws org.w3c.dom.DOMException;
}
