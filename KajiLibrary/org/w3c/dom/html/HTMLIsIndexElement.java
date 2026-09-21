package org.w3c.dom.html;

/**
 * An `<isindex>`. Obsolete in HTML 4.
 */
public interface HTMLIsIndexElement extends HTMLElement {

    /** The form that contains it, or null if it is in none. */
    HTMLFormElement getForm();

    /** The `prompt` attribute. */
    String getPrompt();

    /** It sets the `prompt` attribute. */
    void setPrompt(String prompt);
}
