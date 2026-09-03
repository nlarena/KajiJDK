package org.w3c.dom.html;

import org.w3c.dom.DOMImplementation;

/**
 * La fabrica de documentos HTML.
 *
 * <p>Extiende {@link org.w3c.dom.DOMImplementation} con un solo metodo, y el metodo es la razon de
 * que la interfaz exista: `createHTMLDocument` arma un documento **con su esqueleto puesto**
 * --`html`, `head`, `title` y `body`--, que es lo que distingue a un documento HTML de uno XML
 * vacio.
 */
public interface HTMLDOMImplementation extends org.w3c.dom.DOMImplementation {

    /** Un documento nuevo con su esqueleto puesto y ese titulo. */
    HTMLDocument createHTMLDocument(String title);
}
