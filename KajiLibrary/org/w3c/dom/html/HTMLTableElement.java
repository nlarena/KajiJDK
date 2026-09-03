package org.w3c.dom.html;

import org.w3c.dom.DOMException;

/**
 * Un `<table>`.
 *
 * <p>Los `createTHead`/`createTFoot`/`createCaption` son **idempotentes**: si la seccion ya existe
 * la devuelven en vez de agregar una segunda, porque una tabla no puede tener dos. Los `deleteXxx`
 * que corresponden no fallan si no hay nada que borrar.
 *
 * <p>`getRows` incluye las filas de las tres secciones y en el orden en que se muestran --cabecera,
 * cuerpos, pie--, que no es necesariamente el orden en que estan escritas en el documento.
 *
 * <p>`insertRow(-1)` agrega al final; cualquier otro indice fuera de rango es
 * `INDEX_SIZE_ERR`.
 */
public interface HTMLTableElement extends HTMLElement {

    /** El `<caption>` de la tabla, o nulo si no tiene. */
    HTMLTableCaptionElement getCaption();

    /** Fija el `<caption>` de la tabla, o nulo si no tiene. */
    void setCaption(HTMLTableCaptionElement caption);

    /** El `<thead>`, o nulo si no tiene. */
    HTMLTableSectionElement getTHead();

    /** Fija el `<thead>`, o nulo si no tiene. */
    void setTHead(HTMLTableSectionElement tHead);

    /** El `<tfoot>`, o nulo si no tiene. */
    HTMLTableSectionElement getTFoot();

    /** Fija el `<tfoot>`, o nulo si no tiene. */
    void setTFoot(HTMLTableSectionElement tFoot);

    /** Las filas, en una coleccion viva. */
    HTMLCollection getRows();

    /** Los `<tbody>`, en una coleccion viva. */
    HTMLCollection getTBodies();

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);

    /** El atributo `bgColor`. */
    String getBgColor();

    /** Fija el atributo `bgColor`. */
    void setBgColor(String bgColor);

    /** El borde. */
    String getBorder();

    /** Fija el borde. */
    void setBorder(String border);

    /** El atributo `cellPadding`. */
    String getCellPadding();

    /** Fija el atributo `cellPadding`. */
    void setCellPadding(String cellPadding);

    /** El atributo `cellSpacing`. */
    String getCellSpacing();

    /** Fija el atributo `cellSpacing`. */
    void setCellSpacing(String cellSpacing);

    /** El atributo `frame`. */
    String getFrame();

    /** Fija el atributo `frame`. */
    void setFrame(String frame);

    /** El atributo `rules`. */
    String getRules();

    /** Fija el atributo `rules`. */
    void setRules(String rules);

    /** El atributo `summary`. */
    String getSummary();

    /** Fija el atributo `summary`. */
    void setSummary(String summary);

    /** El ancho. */
    String getWidth();

    /** Fija el ancho. */
    void setWidth(String width);

    /** La cabecera; si ya hay una la devuelve en vez de agregar otra. */
    HTMLElement createTHead();

    /** Borra la cabecera. Si no hay, no hace nada. */
    void deleteTHead();

    /** El pie; si ya hay uno lo devuelve en vez de agregar otro. */
    HTMLElement createTFoot();

    /** Borra el pie. Si no hay, no hace nada. */
    void deleteTFoot();

    /** El titulo; si ya hay uno lo devuelve en vez de agregar otro. */
    HTMLElement createCaption();

    /** Borra el titulo. Si no hay, no hace nada. */
    void deleteCaption();

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
