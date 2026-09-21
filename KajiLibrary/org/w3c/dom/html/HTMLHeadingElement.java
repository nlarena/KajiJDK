package org.w3c.dom.html;

/**
 * An `<h1>` to `<h6>`. The six levels are the same interface; which one it is is known by the name
 * of the tag, not by a property.
 */
public interface HTMLHeadingElement extends HTMLElement {

    /** The alignment (presentational; HTML 4 deprecates it). */
    String getAlign();

    /** It sets the alignment (presentational; HTML 4 deprecates it). */
    void setAlign(String align);
}
