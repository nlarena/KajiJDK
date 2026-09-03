package org.w3c.dom.html;

import org.w3c.dom.Document;

/**
 * Un `<frame>`.
 *
 * <p>`getContentDocument` devuelve el documento cargado adentro, o nulo si no hay ninguno o si es
 * de otro origen: eso ultimo no es una limitacion de la implementacion sino la regla de mismo
 * origen.
 */
public interface HTMLFrameElement extends HTMLElement {

    /** El atributo `frameBorder`. */
    String getFrameBorder();

    /** Fija el atributo `frameBorder`. */
    void setFrameBorder(String frameBorder);

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

    /** El atributo `noResize`. */
    boolean getNoResize();

    /** Fija el atributo `noResize`. */
    void setNoResize(boolean noResize);

    /** El atributo `scrolling`. */
    String getScrolling();

    /** Fija el atributo `scrolling`. */
    void setScrolling(String scrolling);

    /** El origen. */
    String getSrc();

    /** Fija el origen. */
    void setSrc(String src);

    /** El documento cargado adentro, o nulo si no hay o es de otro origen. */
    Document getContentDocument();
}
