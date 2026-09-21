package org.w3c.dom.html;

/**
 * A `<meta>`.
 */
public interface HTMLMetaElement extends HTMLElement {

    /** The `content` attribute. */
    String getContent();

    /** It sets the `content` attribute. */
    void setContent(String content);

    /** The `httpEquiv` attribute. */
    String getHttpEquiv();

    /** It sets the `httpEquiv` attribute. */
    void setHttpEquiv(String httpEquiv);

    /** The `name` attribute. */
    String getName();

    /** It sets the `name` attribute. */
    void setName(String name);

    /** The `scheme` attribute. */
    String getScheme();

    /** It sets the `scheme` attribute. */
    void setScheme(String scheme);
}
