package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * Una regla de una hoja de estilos: la raiz de las siete formas que CSS 2 define.
 *
 * <p>Cual de las siete es se pregunta con {@link #getType} y **no** con `instanceof`. Los dos
 * funcionan, pero el tipo numerico es el que sobrevive a una implementacion que no use la jerarquia
 * de clases que uno espera, y es lo que la especificacion define.
 *
 * <p>`getCssText` devuelve la regla entera como texto, incluido su selector y sus llaves.
 * Asignarlo reemplaza la regla completa, no le agrega: una regla es indivisible desde afuera.
 */
public interface CSSRule {

    /** Una regla que esta implementacion no reconoce. */
    public static final short UNKNOWN_RULE = 0;
    /** Una regla de estilo: un selector y sus declaraciones. */
    public static final short STYLE_RULE = 1;
    /** Un `@charset`. */
    public static final short CHARSET_RULE = 2;
    /** Un `@import`. */
    public static final short IMPORT_RULE = 3;
    /** Un `@media`. */
    public static final short MEDIA_RULE = 4;
    /** Un `@font-face`. */
    public static final short FONT_FACE_RULE = 5;
    /** Un `@page`. */
    public static final short PAGE_RULE = 6;

    /** Cual de las siete formas es esta regla. */
    short getType();

    /** La regla entera como texto. */
    String getCssText();

    /**
     * Reemplaza la regla entera con ese texto.
     *
     * @throws DOMException `SYNTAX_ERR` si el texto no parsea; `INVALID_MODIFICATION_ERR` si
     *     describe una regla de otro tipo que la actual; `NO_MODIFICATION_ALLOWED_ERR` si la regla
     *     es de solo lectura
     */
    void setCssText(String cssText) throws DOMException;

    /** La hoja que la contiene, o nulo. */
    CSSStyleSheet getParentStyleSheet();

    /** La regla que la contiene --solo un `@media` contiene otras--, o nulo. */
    CSSRule getParentRule();
}
