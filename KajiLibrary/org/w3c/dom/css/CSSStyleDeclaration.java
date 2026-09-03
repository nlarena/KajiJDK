package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * Un bloque de declaraciones: lo que va entre las llaves de una regla, o el `style=""` de un
 * elemento.
 *
 * <p>Se puede ver de dos formas y las dos son la misma: como texto (`getCssText`) o como un
 * conjunto de propiedades. Es **vivo**: escribir una propiedad cambia el texto y al reves.
 *
 * <p>Dos formas de leer una propiedad, y la diferencia importa. `getPropertyValue` da el texto y
 * anda siempre; `getPropertyCSSValue` da el valor estructurado y **devuelve nulo para las
 * propiedades abreviadas** --`background`, `font`, `margin`--, porque el valor de una abreviada no
 * es un valor sino varios. Quien quiera esos tiene que pedir las propiedades largas una por una.
 *
 * <p>`item(i)` recorre los **nombres** de las propiedades escritas, en el orden del documento; es
 * lo que permite listar un bloque sin saber de antemano que tiene.
 */
public interface CSSStyleDeclaration {

    /** El bloque entero como texto. */
    String getCssText();

    /**
     * Reemplaza el bloque entero.
     *
     * @throws DOMException `SYNTAX_ERR` si no parsea; `NO_MODIFICATION_ALLOWED_ERR` si el bloque
     *     es de solo lectura
     */
    void setCssText(String cssText) throws DOMException;

    /** El valor de esa propiedad como texto, o la cadena vacia si no esta escrita. */
    String getPropertyValue(String propertyName);

    /** El valor estructurado, o nulo. Ver la nota sobre las abreviadas. */
    CSSValue getPropertyCSSValue(String propertyName);

    /**
     * Saca esa propiedad y devuelve el valor que tenia, o la cadena vacia si no estaba.
     *
     * @throws DOMException `NO_MODIFICATION_ALLOWED_ERR` si el bloque es de solo lectura
     */
    String removeProperty(String propertyName) throws DOMException;

    /** `"important"` si la propiedad lo lleva, la cadena vacia si no. */
    String getPropertyPriority(String propertyName);

    /**
     * Escribe esa propiedad. `priority` es `"important"` o la cadena vacia.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no parsea; `NO_MODIFICATION_ALLOWED_ERR` si el
     *     bloque o esa propiedad son de solo lectura
     */
    void setProperty(String propertyName, String value, String priority) throws DOMException;

    /** Cuantas propiedades hay escritas. */
    int getLength();

    /** El nombre de la propiedad en esa posicion, o la cadena vacia si el indice no vale. */
    String item(int index);

    /** La regla que contiene este bloque, o nulo si es el `style` de un elemento. */
    CSSRule getParentRule();
}
