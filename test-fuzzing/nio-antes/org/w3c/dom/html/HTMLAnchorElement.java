package org.w3c.dom.html;

/**
 * An `<a>`.
 */
public interface HTMLAnchorElement extends HTMLElement {

    /** The keyboard shortcut. */
    String getAccessKey();

    /** It sets the keyboard shortcut. */
    void setAccessKey(String accessKey);

    /** The encoding of the destination. */
    String getCharset();

    /** It sets the encoding of the destination. */
    void setCharset(String charset);

    /** The coordinates. */
    String getCoords();

    /** It sets the coordinates. */
    void setCoords(String coords);

    /** The destination. */
    String getHref();

    /** It sets the destination. */
    void setHref(String href);

    /** The language of the destination. */
    String getHreflang();

    /** It sets the language of the destination. */
    void setHreflang(String hreflang);

    /** The `name` attribute. */
    String getName();

    /** It sets the `name` attribute. */
    void setName(String name);

    /** The relationship with the destination. */
    String getRel();

    /** It sets the relationship with the destination. */
    void setRel(String rel);

    /** The reverse relationship. */
    String getRev();

    /** It sets the reverse relationship. */
    void setRev(String rev);

    /** The shape of the region. */
    String getShape();

    /** It sets the shape of the region. */
    void setShape(String shape);

    /** The position in the tabbing order. */
    int getTabIndex();

    /** It sets the position in the tabbing order. */
    void setTabIndex(int tabIndex);

    /** The target frame. */
    String getTarget();

    /** It sets the target frame. */
    void setTarget(String target);

    /** The type of the control. */
    String getType();

    /** It sets the type of the control. */
    void setType(String type);

    /** It takes the focus away from it. */
    void blur();

    /** It gives it the focus. */
    void focus();
}
