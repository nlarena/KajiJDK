package org.w3c.dom.html;

/**
 * Un `<a>`.
 */
public interface HTMLAnchorElement extends HTMLElement {

    /** El atajo de teclado. */
    String getAccessKey();

    /** Fija el atajo de teclado. */
    void setAccessKey(String accessKey);

    /** La codificacion del destino. */
    String getCharset();

    /** Fija la codificacion del destino. */
    void setCharset(String charset);

    /** Las coordenadas. */
    String getCoords();

    /** Fija las coordenadas. */
    void setCoords(String coords);

    /** El destino. */
    String getHref();

    /** Fija el destino. */
    void setHref(String href);

    /** El idioma del destino. */
    String getHreflang();

    /** Fija el idioma del destino. */
    void setHreflang(String hreflang);

    /** El atributo `name`. */
    String getName();

    /** Fija el atributo `name`. */
    void setName(String name);

    /** La relacion con el destino. */
    String getRel();

    /** Fija la relacion con el destino. */
    void setRel(String rel);

    /** La relacion inversa. */
    String getRev();

    /** Fija la relacion inversa. */
    void setRev(String rev);

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

    /** El tipo del control. */
    String getType();

    /** Fija el tipo del control. */
    void setType(String type);

    /** Le saca el foco. */
    void blur();

    /** Le da el foco. */
    void focus();
}
