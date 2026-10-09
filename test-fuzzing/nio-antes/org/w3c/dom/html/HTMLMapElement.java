package org.w3c.dom.html;

/**
 * A `<map>`.
 */
public interface HTMLMapElement extends HTMLElement {

    /** The `<area>`s of the map, in a live collection. */
    HTMLCollection getAreas();

    /** The `name` attribute. */
    String getName();

    /** It sets the `name` attribute. */
    void setName(String name);
}
