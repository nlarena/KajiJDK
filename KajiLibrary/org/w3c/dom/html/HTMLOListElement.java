package org.w3c.dom.html;

/**
 * Una `<ol>`.
 */
public interface HTMLOListElement extends HTMLElement {

    /** El atributo `compact`. */
    boolean getCompact();

    /** Fija el atributo `compact`. */
    void setCompact(boolean compact);

    /** El atributo `start`. */
    int getStart();

    /** Fija el atributo `start`. */
    void setStart(int start);

    /** El tipo del control. */
    String getType();

    /** Fija el tipo del control. */
    void setType(String type);
}
