package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * Un `@charset`: la codificacion de la hoja.
 *
 * <p>Solo puede estar al principio y solo puede haber uno. Por eso `setEncoding` es lo unico que se
 * puede tocar: mover la regla o agregar una segunda no describiria una hoja valida.
 */
public interface CSSCharsetRule extends CSSRule {

    /** La codificacion declarada. */
    String getEncoding();

    /**
     * Cambia la codificacion.
     *
     * @throws DOMException `SYNTAX_ERR` si el valor no es un nombre de codificacion valido;
     *     `NO_MODIFICATION_ALLOWED_ERR` si la regla es de solo lectura
     */
    void setEncoding(String encoding) throws DOMException;
}
