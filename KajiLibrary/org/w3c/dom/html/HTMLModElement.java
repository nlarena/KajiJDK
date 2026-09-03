package org.w3c.dom.html;

/**
 * Un `<ins>` o un `<del>`.
 */
public interface HTMLModElement extends HTMLElement {

    /** El atributo `cite`. */
    String getCite();

    /** Fija el atributo `cite`. */
    void setCite(String cite);

    /** El atributo `dateTime`. */
    String getDateTime();

    /** Fija el atributo `dateTime`. */
    void setDateTime(String dateTime);
}
