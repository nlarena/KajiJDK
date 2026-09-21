package org.w3c.dom.html;

/**
 * A `<base>`.
 */
public interface HTMLBaseElement extends HTMLElement {

    /** The destination. */
    String getHref();

    /** It sets the destination. */
    void setHref(String href);

    /** The target frame. */
    String getTarget();

    /** It sets the target frame. */
    void setTarget(String target);
}
