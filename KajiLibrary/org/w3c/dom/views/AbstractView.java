package org.w3c.dom.views;

/**
 * KajiLibrary's org.w3c.dom.views.AbstractView -- una vista de un documento.
 *
 * <p>Un mismo documento se puede estar mostrando de varias formas a la vez: una ventana, una
 * impresion, un lector de pantalla. Cada una de esas es una vista, y la interfaz existe para poder
 * <b>nombrarlas</b> sin decir nada sobre ellas -- por eso solo tiene un metodo, y es el que devuelve
 * el documento del que es vista.
 *
 * <p>El modulo Views del DOM nunca creció mas alla de este par de interfaces. Sigue en el API porque
 * {@code UIEvent} necesita decir en <b>que</b> vista ocurrio un evento, y sin este tipo no habria
 * como.
 */
public interface AbstractView {

    /** El documento del que esta es una vista. */
    DocumentView getDocument();
}
