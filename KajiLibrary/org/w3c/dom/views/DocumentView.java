package org.w3c.dom.views;

/**
 * Un documento que sabe cual es su vista por omision.
 *
 * <p>Es la contraparte de {@link AbstractView}: uno va del documento a la vista y el otro de la
 * vista al documento. Un documento puede tener muchas vistas y solo una por omision.
 */
public interface DocumentView {

    /** La vista por omision, o nulo si el documento no tiene ninguna. */
    AbstractView getDefaultView();
}
