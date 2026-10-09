package org.w3c.dom.html;

/**
 * An `<ul>`.
 */
public interface HTMLUListElement extends HTMLElement {

    /** The `compact` attribute. */
    boolean getCompact();

    /** It sets the `compact` attribute. */
    void setCompact(boolean compact);

    /** The type of the control. */
    String getType();

    /** It sets the type of the control. */
    void setType(String type);
}
