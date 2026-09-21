package org.w3c.dom.html;

import org.w3c.dom.Document;

/**
 * A `<frame>`.
 *
 * <p>`getContentDocument` returns the document loaded inside, or null if there is none or if it is
 * from another origin: the latter is not a limitation of the implementation but the same-origin
 * rule.
 */
public interface HTMLFrameElement extends HTMLElement {

    /** The `frameBorder` attribute. */
    String getFrameBorder();

    /** It sets the `frameBorder` attribute. */
    void setFrameBorder(String frameBorder);

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

    /** The `noResize` attribute. */
    boolean getNoResize();

    /** It sets the `noResize` attribute. */
    void setNoResize(boolean noResize);

    /** The `scrolling` attribute. */
    String getScrolling();

    /** It sets the `scrolling` attribute. */
    void setScrolling(String scrolling);

    /** The source. */
    String getSrc();

    /** It sets the source. */
    void setSrc(String src);

    /** The document loaded inside, or null if there is none or it is from another origin. */
    Document getContentDocument();
}
