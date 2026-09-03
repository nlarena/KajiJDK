package org.w3c.dom.html;

/**
 * Un `<meta>`.
 */
public interface HTMLMetaElement extends HTMLElement {

    /** El atributo `content`. */
    String getContent();

    /** Fija el atributo `content`. */
    void setContent(String content);

    /** El atributo `httpEquiv`. */
    String getHttpEquiv();

    /** Fija el atributo `httpEquiv`. */
    void setHttpEquiv(String httpEquiv);

    /** El atributo `name`. */
    String getName();

    /** Fija el atributo `name`. */
    void setName(String name);

    /** El atributo `scheme`. */
    String getScheme();

    /** Fija el atributo `scheme`. */
    void setScheme(String scheme);
}
