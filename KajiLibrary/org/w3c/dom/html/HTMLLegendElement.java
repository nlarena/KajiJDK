package org.w3c.dom.html;

/**
 * Un `<legend>`.
 */
public interface HTMLLegendElement extends HTMLElement {

    /** El formulario que lo contiene, o nulo si no esta en ninguno. */
    HTMLFormElement getForm();

    /** El atajo de teclado. */
    String getAccessKey();

    /** Fija el atajo de teclado. */
    void setAccessKey(String accessKey);

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);
}
