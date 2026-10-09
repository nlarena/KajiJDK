package org.w3c.dom.html;

/**
 * A `<link>`.
 */
public interface HTMLLinkElement extends HTMLElement {

    /** Whether it is disabled. */
    boolean getDisabled();

    /** It sets whether it is disabled. */
    void setDisabled(boolean disabled);

    /** The encoding of the destination. */
    String getCharset();

    /** It sets the encoding of the destination. */
    void setCharset(String charset);

    /** The destination. */
    String getHref();

    /** It sets the destination. */
    void setHref(String href);

    /** The language of the destination. */
    String getHreflang();

    /** It sets the language of the destination. */
    void setHreflang(String hreflang);

    /** The `media` attribute. */
    String getMedia();

    /** It sets the `media` attribute. */
    void setMedia(String media);

    /** The relationship with the destination. */
    String getRel();

    /** It sets the relationship with the destination. */
    void setRel(String rel);

    /** The reverse relationship. */
    String getRev();

    /** It sets the reverse relationship. */
    void setRev(String rev);

    /** The target frame. */
    String getTarget();

    /** It sets the target frame. */
    void setTarget(String target);

    /** The type of the control. */
    String getType();

    /** It sets the type of the control. */
    void setType(String type);
}
