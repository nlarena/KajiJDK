package org.w3c.dom.css;

import org.w3c.dom.Element;
import org.w3c.dom.stylesheets.DocumentStyle;

/**
 * Un documento que admite estilos de anulacion.
 *
 * <p>El estilo de anulacion es una capa que gana sobre todas las hojas y sobre el `style` del
 * elemento: la cascada del usuario, en terminos de CSS 2. Se lo pide vacio y se lo escribe, y a
 * partir de ahi lo que diga tiene la ultima palabra.
 */
public interface DocumentCSS extends DocumentStyle {

    /**
     * El bloque de anulacion de ese elemento, para leerlo o escribirlo.
     *
     * @param pseudoElt el pseudo-elemento --`:first-line`--, o la cadena vacia
     */
    CSSStyleDeclaration getOverrideStyle(Element elt, String pseudoElt);
}
