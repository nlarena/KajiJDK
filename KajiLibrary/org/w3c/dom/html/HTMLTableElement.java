package org.w3c.dom.html;

import org.w3c.dom.DOMException;

/**
 * A `<table>`.
 *
 * <p>The `createTHead`/`createTFoot`/`createCaption` are **idempotent**: if the section already
 * exists they return it instead of adding a second one, because a table cannot have two. The
 * matching `deleteXxx` do not fail if there is nothing to delete.
 *
 * <p>`getRows` includes the rows of the three sections and in the order in which they are shown
 * --header, bodies, footer--, which is not necessarily the order in which they are written in the
 * document.
 *
 * <p>`insertRow(-1)` appends at the end; any other index out of range is `INDEX_SIZE_ERR`.
 */
public interface HTMLTableElement extends HTMLElement {

    /** The `<caption>` of the table, or null if it has none. */
    HTMLTableCaptionElement getCaption();

    /** It sets the `<caption>` of the table, or null if it has none. */
    void setCaption(HTMLTableCaptionElement caption);

    /** The `<thead>`, or null if it has none. */
    HTMLTableSectionElement getTHead();

    /** It sets the `<thead>`, or null if it has none. */
    void setTHead(HTMLTableSectionElement tHead);

    /** The `<tfoot>`, or null if it has none. */
    HTMLTableSectionElement getTFoot();

    /** It sets the `<tfoot>`, or null if it has none. */
    void setTFoot(HTMLTableSectionElement tFoot);

    /** The rows, in a live collection. */
    HTMLCollection getRows();

    /** The `<tbody>`s, in a live collection. */
    HTMLCollection getTBodies();

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);

    /** The `bgColor` attribute. */
    String getBgColor();

    /** It sets the `bgColor` attribute. */
    void setBgColor(String bgColor);

    /** The border. */
    String getBorder();

    /** It sets the border. */
    void setBorder(String border);

    /** The `cellPadding` attribute. */
    String getCellPadding();

    /** It sets the `cellPadding` attribute. */
    void setCellPadding(String cellPadding);

    /** The `cellSpacing` attribute. */
    String getCellSpacing();

    /** It sets the `cellSpacing` attribute. */
    void setCellSpacing(String cellSpacing);

    /** The `frame` attribute. */
    String getFrame();

    /** It sets the `frame` attribute. */
    void setFrame(String frame);

    /** The `rules` attribute. */
    String getRules();

    /** It sets the `rules` attribute. */
    void setRules(String rules);

    /** The `summary` attribute. */
    String getSummary();

    /** It sets the `summary` attribute. */
    void setSummary(String summary);

    /** The width. */
    String getWidth();

    /** It sets the width. */
    void setWidth(String width);

    /** The header; if there already is one it returns it instead of adding another. */
    HTMLElement createTHead();

    /** It deletes the header. If there is none, it does nothing. */
    void deleteTHead();

    /** The footer; if there already is one it returns it instead of adding another. */
    HTMLElement createTFoot();

    /** It deletes the footer. If there is none, it does nothing. */
    void deleteTFoot();

    /** The caption; if there already is one it returns it instead of adding another. */
    HTMLElement createCaption();

    /** It deletes the caption. If there is none, it does nothing. */
    void deleteCaption();

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
