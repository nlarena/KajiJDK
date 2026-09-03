package org.w3c.dom.html;

/**
 * Un `<link>`.
 */
public interface HTMLLinkElement extends HTMLElement {

    /** Si esta deshabilitado. */
    boolean getDisabled();

    /** Fija si esta deshabilitado. */
    void setDisabled(boolean disabled);

    /** La codificacion del destino. */
    String getCharset();

    /** Fija la codificacion del destino. */
    void setCharset(String charset);

    /** El destino. */
    String getHref();

    /** Fija el destino. */
    void setHref(String href);

    /** El idioma del destino. */
    String getHreflang();

    /** Fija el idioma del destino. */
    void setHreflang(String hreflang);

    /** El atributo `media`. */
    String getMedia();

    /** Fija el atributo `media`. */
    void setMedia(String media);

    /** La relacion con el destino. */
    String getRel();

    /** Fija la relacion con el destino. */
    void setRel(String rel);

    /** La relacion inversa. */
    String getRev();

    /** Fija la relacion inversa. */
    void setRev(String rev);

    /** El marco de destino. */
    String getTarget();

    /** Fija el marco de destino. */
    void setTarget(String target);

    /** El tipo del control. */
    String getType();

    /** Fija el tipo del control. */
    void setType(String type);
}
