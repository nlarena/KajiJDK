package org.w3c.dom.html;

/**
 * A `<label>`.
 */
public interface HTMLLabelElement extends HTMLElement {

    /** The form that contains it, or null if it is in none. */
    HTMLFormElement getForm();

    /** The keyboard shortcut. */
    String getAccessKey();

    /** It sets the keyboard shortcut. */
    void setAccessKey(String accessKey);

    /** The `htmlFor` attribute. */
    String getHtmlFor();

    /** It sets the `htmlFor` attribute. */
    void setHtmlFor(String htmlFor);
}
