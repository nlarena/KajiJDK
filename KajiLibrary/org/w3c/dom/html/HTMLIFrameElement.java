package org.w3c.dom.html;

import org.w3c.dom.Document;

/**
 * Un `<iframe>`. Vale la nota de `getContentDocument` de
 * {@link HTMLFrameElement}.
 */
public interface HTMLIFrameElement extends HTMLElement {

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);

    /** El atributo `frameBorder`. */
    String getFrameBorder();

    /** Fija el atributo `frameBorder`. */
    void setFrameBorder(String frameBorder);

    /** El alto. */
    String getHeight();

    /** Fija el alto. */
    void setHeight(String height);

    /** El atributo `longDesc`. */
    String getLongDesc();

    /** Fija el atributo `longDesc`. */
    void setLongDesc(String longDesc);

    /** El atributo `marginHeight`. */
    String getMarginHeight();

    /** Fija el atributo `marginHeight`. */
    void setMarginHeight(String marginHeight);

    /** El atributo `marginWidth`. */
    String getMarginWidth();

    /** Fija el atributo `marginWidth`. */
    void setMarginWidth(String marginWidth);

    /** El atributo `name`. */
    String getName();

    /** Fija el atributo `name`. */
    void setName(String name);

    /** El atributo `scrolling`. */
    String getScrolling();

    /** Fija el atributo `scrolling`. */
    void setScrolling(String scrolling);

    /** El origen. */
    String getSrc();

    /** Fija el origen. */
    void setSrc(String src);

    /** El ancho. */
    String getWidth();

    /** Fija el ancho. */
    void setWidth(String width);

    /** El documento cargado adentro, o nulo si no hay o es de otro origen. */
    Document getContentDocument();
}
