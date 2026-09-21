package org.w3c.dom.html;

/**
 * A `<fieldset>`.
 */
public interface HTMLFieldSetElement extends HTMLElement {

    /** The form that contains it, or null if it is in none. */
    HTMLFormElement getForm();
}
