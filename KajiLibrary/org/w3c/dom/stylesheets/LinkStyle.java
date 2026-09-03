package org.w3c.dom.stylesheets;

/**
 * Un nodo que enlaza o contiene una hoja de estilos: un `<link>` o un `<style>`.
 *
 * <p>Lo implementan los elementos, no el documento. De ahi que sea la contraparte de
 * {@link StyleSheet#getOwnerNode}: uno va del nodo a la hoja y el otro al reves.
 */
public interface LinkStyle {

    /** La hoja que este nodo aporta, o nulo si todavia no se cargo o no es valida. */
    StyleSheet getSheet();
}
