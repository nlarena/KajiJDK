package org.w3c.dom.html;

/**
 * A `<li>`.
 */
public interface HTMLLIElement extends HTMLElement {

    /** The type of the control. */
    String getType();

    /** It sets the type of the control. */
    void setType(String type);

    /** The current value. */
    int getValue();

    /** It sets the current value. */
    void setValue(int value);
}
