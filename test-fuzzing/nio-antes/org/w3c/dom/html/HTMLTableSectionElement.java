package org.w3c.dom.html;

import org.w3c.dom.DOMException;

/**
 * A `<thead>`, `<tbody>` or `<tfoot>`. The three are the same interface.
 */
public interface HTMLTableSectionElement extends HTMLElement {

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);

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

    /** The rows, in a live collection. */
    HTMLCollection getRows();

    /**
     * It inserts a row at that position; -1 appends at the end.
     *
     * @throws DOMException `INDEX_SIZE_ERR` if the index is out of range
     */
    HTMLElement insertRow(int index) throws org.w3c.dom.DOMException;

    /**
     * It deletes the row at that position; -1 deletes the last one.
     *
     * @throws DOMException `INDEX_SIZE_ERR` if the index is out of range
     */
    void deleteRow(int index) throws org.w3c.dom.DOMException;
}
