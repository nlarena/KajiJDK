package org.w3c.dom.html;

/**
 * An `<optgroup>`.
 */
public interface HTMLOptGroupElement extends HTMLElement {

    /** Whether it is disabled. */
    boolean getDisabled();

    /** It sets whether it is disabled. */
    void setDisabled(boolean disabled);

    /** The `label` attribute. */
    String getLabel();

    /** It sets the `label` attribute. */
    void setLabel(String label);
}
