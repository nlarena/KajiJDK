package org.w3c.dom.css;

import org.w3c.dom.Element;
import org.w3c.dom.views.AbstractView;

/**
 * Una vista que sabe calcular el estilo **computado** de un elemento.
 *
 * <p>El estilo computado es el resultado de aplicar la cascada entera --las hojas del autor, las
 * del usuario, las del navegador, la herencia y el `style` del elemento-- y por eso es de **solo
 * lectura**: es una conclusion, no una fuente. Escribirle no tendria a quien afectar.
 *
 * <p>Es un metodo de la **vista** y no del elemento porque el resultado depende del medio: la misma
 * regla da un `font-size` distinto en pantalla que en papel.
 */
public interface ViewCSS extends AbstractView {

    /**
     * El estilo computado de ese elemento en esta vista.
     *
     * @param pseudoElt el pseudo-elemento --`:first-line`--, o la cadena vacia
     */
    CSSStyleDeclaration getComputedStyle(Element elt, String pseudoElt);
}
