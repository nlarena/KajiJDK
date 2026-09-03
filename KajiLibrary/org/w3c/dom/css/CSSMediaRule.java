package org.w3c.dom.css;

import org.w3c.dom.DOMException;
import org.w3c.dom.stylesheets.MediaList;

/**
 * Un `@media`: reglas que solo aplican a ciertos medios.
 *
 * <p>Es la unica regla que contiene otras, y por eso es la unica que tiene `insertRule` y
 * `deleteRule` ademas de la hoja.
 */
public interface CSSMediaRule extends CSSRule {

    /** Los medios para los que aplican las reglas de adentro. */
    MediaList getMedia();

    /** Las reglas de adentro, en una lista viva. */
    CSSRuleList getCssRules();

    /**
     * Inserta esa regla en esa posicion y devuelve la posicion donde quedo.
     *
     * @throws DOMException `HIERARCHY_REQUEST_ERR` si la regla no puede ir dentro de un `@media`
     *     --un `@import` o un `@charset`, por ejemplo--; `INDEX_SIZE_ERR` si el indice esta fuera
     *     de rango; `SYNTAX_ERR` si el texto no parsea
     */
    int insertRule(String rule, int index) throws DOMException;

    /**
     * Borra la regla de esa posicion.
     *
     * @throws DOMException `INDEX_SIZE_ERR` si el indice esta fuera de rango
     */
    void deleteRule(int index) throws DOMException;
}
