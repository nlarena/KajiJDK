package org.w3c.dom.html;

/**
 * Una `<ul>`.
 */
public interface HTMLUListElement extends HTMLElement {

    /** El atributo `compact`. */
    boolean getCompact();

    /** Fija el atributo `compact`. */
    void setCompact(boolean compact);

    /** El tipo del control. */
    String getType();

    /** Fija el tipo del control. */
    void setType(String type);
}
