package org.w3c.dom.css;

import org.w3c.dom.stylesheets.MediaList;

/**
 * Un `@import`: otra hoja traida a esta.
 *
 * <p>`getStyleSheet` puede devolver nulo y hay varias razones legitimas: la hoja todavia no se
 * descargo, no se pudo descargar, o el medio del `@import` no aplica al medio actual. Ninguna es un
 * error, y por eso no hay excepcion.
 */
public interface CSSImportRule extends CSSRule {

    /** La URI de la hoja importada. */
    String getHref();

    /** Los medios para los que aplica la importacion. Vacia significa todos. */
    MediaList getMedia();

    /** La hoja importada, o nulo. Ver la nota de la clase. */
    CSSStyleSheet getStyleSheet();
}
