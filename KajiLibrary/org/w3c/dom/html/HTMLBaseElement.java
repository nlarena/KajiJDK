package org.w3c.dom.html;

/**
 * Un `<base>`.
 */
public interface HTMLBaseElement extends HTMLElement {

    /** El destino. */
    String getHref();

    /** Fija el destino. */
    void setHref(String href);

    /** El marco de destino. */
    String getTarget();

    /** Fija el marco de destino. */
    void setTarget(String target);
}
