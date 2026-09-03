package org.w3c.dom.css;

import org.w3c.dom.DOMException;
import org.w3c.dom.stylesheets.StyleSheet;

/**
 * Una hoja de estilos CSS: la parte de {@link StyleSheet} que si sabe de reglas.
 *
 * <p>`getOwnerRule` y `getOwnerNode` --el heredado-- son excluyentes, como en `StyleSheet`: una
 * hoja importada tiene regla dueno y ninguna nodo; una enlazada, al reves.
 *
 * <p>`insertRule` devuelve la posicion donde quedo la regla, que no siempre es el indice que se
 * pidio: las reglas `@charset` e `@import` tienen que ir antes que las demas, y la implementacion
 * puede acomodarlas.
 */
public interface CSSStyleSheet extends StyleSheet {

    /** El `@import` que trajo esta hoja, o nulo si esta enlazada desde el documento. */
    CSSRule getOwnerRule();

    /** Las reglas de la hoja, en una lista viva. */
    CSSRuleList getCssRules();

    /**
     * Inserta esa regla en esa posicion y devuelve donde quedo.
     *
     * @throws DOMException `HIERARCHY_REQUEST_ERR` si la regla no puede ir ahi --un `@import`
     *     despues de una regla de estilo, por ejemplo--; `INDEX_SIZE_ERR` si el indice esta fuera
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
