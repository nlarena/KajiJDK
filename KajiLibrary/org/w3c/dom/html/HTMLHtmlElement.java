package org.w3c.dom.html;

/**
 * El `<html>` de la raiz.
 */
public interface HTMLHtmlElement extends HTMLElement {

    /** El atributo `version`. */
    String getVersion();

    /** Fija el atributo `version`. */
    void setVersion(String version);
}
