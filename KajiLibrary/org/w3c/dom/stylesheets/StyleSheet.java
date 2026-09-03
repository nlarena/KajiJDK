package org.w3c.dom.stylesheets;

import org.w3c.dom.Node;

/**
 * Una hoja de estilos, del lenguaje que sea.
 *
 * <p>Es la parte **independiente del lenguaje**: `type`, si esta deshabilitada, de donde salio y
 * para que medios es. Lo que la hoja dice adentro no esta aca -- eso lo agrega la extension de cada
 * lenguaje, y en el caso de CSS es {@link org.w3c.dom.css.CSSStyleSheet}, que suma las reglas.
 *
 * <p>`getOwnerNode` y `getParentStyleSheet` son excluyentes: una hoja o esta enlazada desde el
 * documento --y entonces tiene nodo dueno-- o esta importada desde otra hoja --y entonces tiene
 * hoja padre--. La que no aplica devuelve nulo.
 */
public interface StyleSheet {

    /** El lenguaje de la hoja, por ejemplo `"text/css"`. */
    String getType();

    /** Si esta deshabilitada. Una hoja deshabilitada no afecta al documento. */
    boolean getDisabled();

    /** La habilita o la deshabilita. */
    void setDisabled(boolean disabled);

    /** El nodo que la enlaza --un `<link>` o un `<style>`--, o nulo si vino importada. */
    Node getOwnerNode();

    /** La hoja que la importo, o nulo si esta enlazada desde el documento. */
    StyleSheet getParentStyleSheet();

    /** La URI de donde salio, o nulo si esta escrita en el documento. */
    String getHref();

    /** El titulo que le puso quien la enlazo, o nulo. */
    String getTitle();

    /** Los medios para los que aplica. Vacia significa todos. */
    MediaList getMedia();
}
