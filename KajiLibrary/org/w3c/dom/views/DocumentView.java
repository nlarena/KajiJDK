package org.w3c.dom.views;

/**
 * KajiLibrary's org.w3c.dom.views.DocumentView -- un documento que sabe cual es su vista principal.
 *
 * <p>La implementa el {@code Document}. Es la mitad inversa de {@link AbstractView}: de la vista se
 * llega al documento y del documento a su vista por omision.
 *
 * <p>"Por omision" quiere decir la que se usa cuando nadie dice cual: en un navegador, la ventana.
 * Un documento puede tener muchas vistas y esta interfaz solo nombra una, porque es la unica que se
 * puede elegir sin conocer el medio.
 */
public interface DocumentView {

    /** La vista por omision de este documento. */
    AbstractView getDefaultView();
}
