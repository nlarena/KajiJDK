package org.w3c.dom.html;

/**
 * Un `<img>`.
 */
public interface HTMLImageElement extends HTMLElement {

    /** El atributo `lowSrc`. */
    String getLowSrc();

    /** Fija el atributo `lowSrc`. */
    void setLowSrc(String lowSrc);

    /** El atributo `name`. */
    String getName();

    /** Fija el atributo `name`. */
    void setName(String name);

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);

    /** El texto alternativo. */
    String getAlt();

    /** Fija el texto alternativo. */
    void setAlt(String alt);

    /** El borde. */
    String getBorder();

    /** Fija el borde. */
    void setBorder(String border);

    /** El alto. */
    String getHeight();

    /** Fija el alto. */
    void setHeight(String height);

    /** El atributo `hspace`. */
    String getHspace();

    /** Fija el atributo `hspace`. */
    void setHspace(String hspace);

    /** El atributo `isMap`. */
    boolean getIsMap();

    /** Fija el atributo `isMap`. */
    void setIsMap(boolean isMap);

    /** El atributo `longDesc`. */
    String getLongDesc();

    /** Fija el atributo `longDesc`. */
    void setLongDesc(String longDesc);

    /** El origen. */
    String getSrc();

    /** Fija el origen. */
    void setSrc(String src);

    /** El atributo `useMap`. */
    String getUseMap();

    /** Fija el atributo `useMap`. */
    void setUseMap(String useMap);

    /** El atributo `vspace`. */
    String getVspace();

    /** Fija el atributo `vspace`. */
    void setVspace(String vspace);

    /** El ancho. */
    String getWidth();

    /** Fija el ancho. */
    void setWidth(String width);
}
