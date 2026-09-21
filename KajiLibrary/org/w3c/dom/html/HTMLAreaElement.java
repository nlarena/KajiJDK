package org.w3c.dom.html;

/**
 * An `<area>` of an image map.
 */
public interface HTMLAreaElement extends HTMLElement {

    /** The keyboard shortcut. */
    String getAccessKey();

    /** It sets the keyboard shortcut. */
    void setAccessKey(String accessKey);

    /** The alternative text. */
    String getAlt();

    /** It sets the alternative text. */
    void setAlt(String alt);

    /** The coordinates. */
    String getCoords();

    /** It sets the coordinates. */
    void setCoords(String coords);

    /** The destination. */
    String getHref();

    /** It sets the destination. */
    void setHref(String href);

    /** The `noHref` attribute. */
    boolean getNoHref();

    /** It sets the `noHref` attribute. */
    void setNoHref(boolean noHref);

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
}
