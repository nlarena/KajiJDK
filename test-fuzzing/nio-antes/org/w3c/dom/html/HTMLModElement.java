package org.w3c.dom.html;

/**
 * An `<ins>` or a `<del>`.
 */
public interface HTMLModElement extends HTMLElement {

    /** The `cite` attribute. */
    String getCite();

    /** It sets the `cite` attribute. */
    void setCite(String cite);

    /** The `dateTime` attribute. */
    String getDateTime();

    /** It sets the `dateTime` attribute. */
    void setDateTime(String dateTime);
}
