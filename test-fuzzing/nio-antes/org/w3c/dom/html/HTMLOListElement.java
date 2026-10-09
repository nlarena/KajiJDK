package org.w3c.dom.html;

/**
 * An `<ol>`.
 */
public interface HTMLOListElement extends HTMLElement {

    /** The `compact` attribute. */
    boolean getCompact();

    /** It sets the `compact` attribute. */
    void setCompact(boolean compact);

    /** The `start` attribute. */
    int getStart();

    /** It sets the `start` attribute. */
    void setStart(int start);

    /** The type of the control. */
    String getType();

    /** It sets the type of the control. */
    void setType(String type);
}
