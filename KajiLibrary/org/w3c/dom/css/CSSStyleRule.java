package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * Una regla de estilo: un selector y las declaraciones que le aplica.
 *
 * <p>`getSelectorText` da el selector como texto, incluidos los grupos separados por coma. La
 * especificacion no expone el selector parseado, asi que quien necesite las partes tiene que
 * parsearlo por su cuenta -- es una limitacion de CSS 2, no de esta implementacion.
 */
public interface CSSStyleRule extends CSSRule {

    /** El selector, como texto. */
    String getSelectorText();

    /**
     * Reemplaza el selector.
     *
     * @throws DOMException `SYNTAX_ERR` si no parsea; `NO_MODIFICATION_ALLOWED_ERR` si la regla es
     *     de solo lectura
     */
    void setSelectorText(String selectorText) throws DOMException;

    /** Las declaraciones de esta regla. La lista es viva. */
    CSSStyleDeclaration getStyle();
}
