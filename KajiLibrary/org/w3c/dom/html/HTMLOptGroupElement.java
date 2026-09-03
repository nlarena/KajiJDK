package org.w3c.dom.html;

/**
 * Un `<optgroup>`.
 */
public interface HTMLOptGroupElement extends HTMLElement {

    /** Si esta deshabilitado. */
    boolean getDisabled();

    /** Fija si esta deshabilitado. */
    void setDisabled(boolean disabled);

    /** El atributo `label`. */
    String getLabel();

    /** Fija el atributo `label`. */
    void setLabel(String label);
}
