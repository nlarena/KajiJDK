package org.w3c.dom.css;

/**
 * Un elemento con atributo `style`.
 *
 * <p>El bloque que devuelve es **vivo y escribible**: cambiarlo cambia el atributo del documento.
 * Es lo que distingue este estilo del computado de {@link ViewCSS}, que es de solo lectura porque
 * es un resultado y no una fuente.
 */
public interface ElementCSSInlineStyle {

    /** El bloque del atributo `style`. */
    CSSStyleDeclaration getStyle();
}
