package org.w3c.dom.html;

/**
 * Un `<button>`.
 */
public interface HTMLButtonElement extends HTMLElement {

    /** El formulario que lo contiene, o nulo si no esta en ninguno. */
    HTMLFormElement getForm();

    /** El atajo de teclado. */
    String getAccessKey();

    /** Fija el atajo de teclado. */
    void setAccessKey(String accessKey);

    /** Si esta deshabilitado. */
    boolean getDisabled();

    /** Fija si esta deshabilitado. */
    void setDisabled(boolean disabled);

    /** El atributo `name`. */
    String getName();

    /** Fija el atributo `name`. */
    void setName(String name);

    /** La posicion en el orden de tabulacion. */
    int getTabIndex();

    /** Fija la posicion en el orden de tabulacion. */
    void setTabIndex(int tabIndex);

    /** El tipo del control. */
    String getType();

    /** El valor actual. */
    String getValue();

    /** Fija el valor actual. */
    void setValue(String value);
}
