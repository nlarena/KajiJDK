package org.w3c.dom.html;

/**
 * Un `<h1>` a `<h6>`. Los seis niveles son la misma interfaz; cual es se
 * sabe por el nombre de la etiqueta, no por una propiedad.
 */
public interface HTMLHeadingElement extends HTMLElement {

    /** La alineacion (presentacion; HTML 4 la desaconseja). */
    String getAlign();

    /** Fija la alineacion (presentacion; HTML 4 la desaconseja). */
    void setAlign(String align);
}
