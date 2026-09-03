package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * El valor de una declaracion CSS.
 *
 * <p>Tres formas: un valor primitivo, una lista de valores, o `inherit`. La cuarta constante,
 * `CSS_CUSTOM`, es para lo que una implementacion entienda y el DOM no modele -- una propiedad
 * abreviada como `background`, por ejemplo, cuyo valor no es ni un primitivo ni una lista.
 */
public interface CSSValue {

    /** El valor es la palabra clave `inherit`. */
    public static final short CSS_INHERIT = 0;
    /** Es un {@link CSSPrimitiveValue}. */
    public static final short CSS_PRIMITIVE_VALUE = 1;
    /** Es un {@link CSSValueList}. */
    public static final short CSS_VALUE_LIST = 2;
    /** Es algo que el DOM no modela; ver la nota de la clase. */
    public static final short CSS_CUSTOM = 3;

    /** El valor como texto. */
    String getCssText();

    /**
     * Reemplaza el valor con ese texto.
     *
     * @throws DOMException `SYNTAX_ERR` si no parsea; `INVALID_MODIFICATION_ERR` si el texto
     *     describe un valor de otra forma que la actual; `NO_MODIFICATION_ALLOWED_ERR` si el valor
     *     es de solo lectura
     */
    void setCssText(String cssText) throws DOMException;

    /** Cual de las cuatro formas es. */
    short getCssValueType();
}
