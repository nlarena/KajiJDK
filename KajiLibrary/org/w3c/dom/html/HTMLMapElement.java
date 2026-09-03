package org.w3c.dom.html;

/**
 * Un `<map>`.
 */
public interface HTMLMapElement extends HTMLElement {

    /** Las `<area>` del mapa, en una coleccion viva. */
    HTMLCollection getAreas();

    /** El atributo `name`. */
    String getName();

    /** Fija el atributo `name`. */
    void setName(String name);
}
