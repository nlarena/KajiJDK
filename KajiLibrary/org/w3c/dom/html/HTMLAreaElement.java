package org.w3c.dom.html;

/**
 * Un `<area>` de un mapa de imagen.
 */
public interface HTMLAreaElement extends HTMLElement {

    /** El atajo de teclado. */
    String getAccessKey();

    /** Fija el atajo de teclado. */
    void setAccessKey(String accessKey);

    /** El texto alternativo. */
    String getAlt();

    /** Fija el texto alternativo. */
    void setAlt(String alt);

    /** Las coordenadas. */
    String getCoords();

    /** Fija las coordenadas. */
    void setCoords(String coords);

    /** El destino. */
    String getHref();

    /** Fija el destino. */
    void setHref(String href);

    /** El atributo `noHref`. */
    boolean getNoHref();

    /** Fija el atributo `noHref`. */
    void setNoHref(boolean noHref);

    /** La forma de la region. */
    String getShape();

    /** Fija la forma de la region. */
    void setShape(String shape);

    /** La posicion en el orden de tabulacion. */
    int getTabIndex();

    /** Fija la posicion en el orden de tabulacion. */
    void setTabIndex(int tabIndex);

    /** El marco de destino. */
    String getTarget();

    /** Fija el marco de destino. */
    void setTarget(String target);
}
