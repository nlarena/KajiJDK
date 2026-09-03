package org.w3c.dom.html;

import org.w3c.dom.Element;

/**
 * Lo que todo elemento de un documento HTML tiene, sea cual sea su etiqueta.
 *
 * <p>Los cuatro atributos de aca --`id`, `title`, `lang`, `dir`, `class`-- son los que HTML 4 define
 * para cualquier elemento, y por eso viven en la raiz de la jerarquia y no repetidos en cada
 * subtipo.
 *
 * <p>Ojo con `getClassName`: el atributo se llama `class` en el documento y el metodo `className`
 * en la API. No es un descuido de nadie --`class` es palabra reservada de Java-- y es la unica
 * propiedad de este paquete donde el nombre del metodo y el del atributo no coinciden.
 */
public interface HTMLElement extends org.w3c.dom.Element {

    /** El atributo `id`. */
    String getId();

    /** Fija el atributo `id`. */
    void setId(String id);

    /** El atributo `title`. */
    String getTitle();

    /** Fija el atributo `title`. */
    void setTitle(String title);

    /** El atributo `lang`. */
    String getLang();

    /** Fija el atributo `lang`. */
    void setLang(String lang);

    /** El atributo `dir`. */
    String getDir();

    /** Fija el atributo `dir`. */
    void setDir(String dir);

    /** El atributo `class`. */
    String getClassName();

    /** Fija el atributo `class`. */
    void setClassName(String className);
}
