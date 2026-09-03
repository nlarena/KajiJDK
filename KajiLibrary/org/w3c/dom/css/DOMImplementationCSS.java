package org.w3c.dom.css;

import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;

/**
 * La fabrica de hojas de estilo CSS.
 *
 * <p>Agrega un solo metodo a {@link DOMImplementation}, y es el unico camino para crear una hoja
 * que no venga de un documento: una hoja recien creada no esta enlazada a nada hasta que alguien la
 * ponga en un documento.
 */
public interface DOMImplementationCSS extends DOMImplementation {

    /**
     * Una hoja nueva y vacia, con ese titulo y esos medios.
     *
     * @throws DOMException `SYNTAX_ERR` si la lista de medios no parsea
     */
    CSSStyleSheet createCSSStyleSheet(String title, String media) throws DOMException;
}
