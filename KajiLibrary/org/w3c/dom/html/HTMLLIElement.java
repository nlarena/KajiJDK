package org.w3c.dom.html;

/**
 * Un `<li>`.
 */
public interface HTMLLIElement extends HTMLElement {

    /** El tipo del control. */
    String getType();

    /** Fija el tipo del control. */
    void setType(String type);

    /** El valor actual. */
    int getValue();

    /** Fija el valor actual. */
    void setValue(int value);
}
