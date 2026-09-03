package org.w3c.dom.html;

/**
 * Un `<option>`.
 *
 * <p>`getIndex` es su posicion dentro del `select` que lo contiene, y `getText` el texto que se
 * muestra --que no es el `value` que se envia--.
 */
public interface HTMLOptionElement extends HTMLElement {

    /** El formulario que lo contiene, o nulo si no esta en ninguno. */
    HTMLFormElement getForm();

    /** Si el documento lo selecciona. */
    boolean getDefaultSelected();

    /** Fija si el documento lo selecciona. */
    void setDefaultSelected(boolean defaultSelected);

    /** El texto que se muestra. */
    String getText();

    /** La posicion. */
    int getIndex();

    /** Si esta deshabilitado. */
    boolean getDisabled();

    /** Fija si esta deshabilitado. */
    void setDisabled(boolean disabled);

    /** El atributo `label`. */
    String getLabel();

    /** Fija el atributo `label`. */
    void setLabel(String label);

    /** Si esta seleccionado ahora. */
    boolean getSelected();

    /** Fija si esta seleccionado ahora. */
    void setSelected(boolean selected);

    /** El valor actual. */
    String getValue();

    /** Fija el valor actual. */
    void setValue(String value);
}
