package org.w3c.dom.html;

import org.w3c.dom.Document;

/**
 * An `<iframe>`. The note on `getContentDocument` of {@link HTMLFrameElement} holds.
 */
public interface HTMLIFrameElement extends HTMLElement {

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);

    /** The `frameBorder` attribute. */
    String getFrameBorder();

    /** It sets the `frameBorder` attribute. */
    void setFrameBorder(String frameBorder);

    /** The height. */
    String getHeight();

    /** It sets the height. */
    void setHeight(String height);

    /** The `longDesc` attribute. */
    String getLongDesc();

    /** It sets the `longDesc` attribute. */
    void setLongDesc(String longDesc);

    /** The `marginHeight` attribute. */
    String getMarginHeight();

    /** It sets the `marginHeight` attribute. */
    void setMarginHeight(String marginHeight);

    /** The `marginWidth` attribute. */
    String getMarginWidth();

    /** It sets the `marginWidth` attribute. */
    void setMarginWidth(String marginWidth);

    /** The `name` attribute. */
    String getName();

    /** It sets the `name` attribute. */
    void setName(String name);

    /** The `scrolling` attribute. */
    String getScrolling();

    /** It sets the `scrolling` attribute. */
    void setScrolling(String scrolling);

    /** The source. */
    String getSrc();

    /** It sets the source. */
    void setSrc(String src);

    /** The width. */
    String getWidth();

    /** It sets the width. */
    void setWidth(String width);

    /** The document loaded inside, or null if there is none or it is from another origin. */
    Document getContentDocument();
}
