package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/** Un `@page`: las declaraciones que aplican a una pagina impresa. */
public interface CSSPageRule extends CSSRule {

    /** El selector de la pagina --`:first`, `:left`--, o la cadena vacia. */
    String getSelectorText();

    /**
     * Cambia el selector de pagina.
     *
     * @throws DOMException `SYNTAX_ERR` si no parsea; `NO_MODIFICATION_ALLOWED_ERR` si la regla es
     *     de solo lectura
     */
    void setSelectorText(String selectorText) throws DOMException;

    /** Las declaraciones de la pagina. */
    CSSStyleDeclaration getStyle();
}
