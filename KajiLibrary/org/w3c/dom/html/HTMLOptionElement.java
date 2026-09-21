package org.w3c.dom.html;

/**
 * An `<option>`.
 *
 * <p>`getIndex` is its position inside the `select` that contains it, and `getText` the text that
 * is shown --which is not the `value` that is submitted--.
 */
public interface HTMLOptionElement extends HTMLElement {

    /** The form that contains it, or null if it is in none. */
    HTMLFormElement getForm();

    /** Whether the document selects it. */
    boolean getDefaultSelected();

    /** It sets whether the document selects it. */
    void setDefaultSelected(boolean defaultSelected);

    /** The text that is shown. */
    String getText();

    /** The position. */
    int getIndex();

    /** Whether it is disabled. */
    boolean getDisabled();

    /** It sets whether it is disabled. */
    void setDisabled(boolean disabled);

    /** The `label` attribute. */
    String getLabel();

    /** It sets the `label` attribute. */
    void setLabel(String label);

    /** Whether it is selected now. */
    boolean getSelected();

    /** It sets whether it is selected now. */
    void setSelected(boolean selected);

    /** The current value. */
    String getValue();

    /** It sets the current value. */
    void setValue(String value);
}
