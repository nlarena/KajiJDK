package org.w3c.dom.html;

/**
 * A `<hr>`.
 */
public interface HTMLHRElement extends HTMLElement {

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);

    /** The `noShade` attribute. */
    boolean getNoShade();

    /** It sets the `noShade` attribute. */
    void setNoShade(boolean noShade);

    /** The visible size. */
    String getSize();

    /** It sets the visible size. */
    void setSize(String size);

    /** The width. */
    String getWidth();

    /** It sets the width. */
    void setWidth(String width);
}
