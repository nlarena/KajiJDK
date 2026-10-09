package org.w3c.dom.html;

/**
 * A `<frameset>`.
 */
public interface HTMLFrameSetElement extends HTMLElement {

    /** The `cols` attribute. */
    String getCols();

    /** It sets the `cols` attribute. */
    void setCols(String cols);

    /** The `rows` attribute: the heights of the rows of frames, as a string. */
    String getRows();

    /** It sets the `rows` attribute. */
    void setRows(String rows);
}
