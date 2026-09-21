package org.w3c.dom.html;

/**
 * A `<td>` or a `<th>`. The two are the same interface.
 */
public interface HTMLTableCellElement extends HTMLElement {

    /** The position in its row. */
    int getCellIndex();

    /** The `abbr` attribute. */
    String getAbbr();

    /** It sets the `abbr` attribute. */
    void setAbbr(String abbr);

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);

    /** The `axis` attribute. */
    String getAxis();

    /** It sets the `axis` attribute. */
    void setAxis(String axis);

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

    /** How many columns it spans. */
    int getColSpan();

    /** It sets how many columns it spans. */
    void setColSpan(int colSpan);

    /** The `headers` attribute. */
    String getHeaders();

    /** It sets the `headers` attribute. */
    void setHeaders(String headers);

    /** The height. */
    String getHeight();

    /** It sets the height. */
    void setHeight(String height);

    /** The `noWrap` attribute. */
    boolean getNoWrap();

    /** It sets the `noWrap` attribute. */
    void setNoWrap(boolean noWrap);

    /** How many rows it spans. */
    int getRowSpan();

    /** It sets how many rows it spans. */
    void setRowSpan(int rowSpan);

    /** The `scope` attribute. */
    String getScope();

    /** It sets the `scope` attribute. */
    void setScope(String scope);

    /** The `vAlign` attribute. */
    String getVAlign();

    /** It sets the `vAlign` attribute. */
    void setVAlign(String vAlign);

    /** The width. */
    String getWidth();

    /** It sets the width. */
    void setWidth(String width);
}
