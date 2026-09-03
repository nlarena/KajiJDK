package org.w3c.dom.html;

/**
 * Un `<label>`.
 */
public interface HTMLLabelElement extends HTMLElement {

    /** El formulario que lo contiene, o nulo si no esta en ninguno. */
    HTMLFormElement getForm();

    /** El atajo de teclado. */
    String getAccessKey();

    /** Fija el atajo de teclado. */
    void setAccessKey(String accessKey);

    /** El atributo `htmlFor`. */
    String getHtmlFor();

    /** Fija el atributo `htmlFor`. */
    void setHtmlFor(String htmlFor);
}
