package org.w3c.dom.html;

/**
 * A `<col>` or a `<colgroup>`.
 */
public interface HTMLTableColElement extends HTMLElement {

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

    /** The `span` attribute. */
    int getSpan();

    /** It sets the `span` attribute. */
    void setSpan(int span);

    /** The `vAlign` attribute. */
    String getVAlign();

    /** It sets the `vAlign` attribute. */
    void setVAlign(String vAlign);

    /** The width. */
    String getWidth();

    /** It sets the width. */
    void setWidth(String width);
}
