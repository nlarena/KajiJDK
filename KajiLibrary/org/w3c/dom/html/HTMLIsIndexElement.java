package org.w3c.dom.html;

/**
 * Un `<isindex>`. Obsoleto en HTML 4.
 */
public interface HTMLIsIndexElement extends HTMLElement {

    /** El formulario que lo contiene, o nulo si no esta en ninguno. */
    HTMLFormElement getForm();

    /** El atributo `prompt`. */
    String getPrompt();

    /** Fija el atributo `prompt`. */
    void setPrompt(String prompt);
}
