package org.w3c.dom.html;

/**
 * A `<legend>`.
 */
public interface HTMLLegendElement extends HTMLElement {

    /** The form that contains it, or null if it is in none. */
    HTMLFormElement getForm();

    /** The keyboard shortcut. */
    String getAccessKey();

    /** It sets the keyboard shortcut. */
    void setAccessKey(String accessKey);

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);
}
