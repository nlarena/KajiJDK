package org.w3c.dom.html;

/**
 * Un `<fieldset>`.
 */
public interface HTMLFieldSetElement extends HTMLElement {

    /** El formulario que lo contiene, o nulo si no esta en ninguno. */
    HTMLFormElement getForm();
}
