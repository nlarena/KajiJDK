package org.w3c.dom.html;

/**
 * Un `<div>`.
 */
public interface HTMLDivElement extends HTMLElement {

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);
}
