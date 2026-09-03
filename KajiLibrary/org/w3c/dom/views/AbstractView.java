package org.w3c.dom.views;

/**
 * Una vista de un documento: el contexto en el que se lo mira.
 *
 * <p>La interfaz esta vacia salvo por {@link #getDocument}, y eso es todo lo que el DOM Nivel 2
 * define. La razon es que "vista" es un concepto abstracto a proposito: una ventana de navegador,
 * una hoja impresa y un lector de pantalla son tres vistas del mismo documento, y lo unico que
 * tienen en comun es que hay un documento detras.
 *
 * <p>Lo que le da contenido son las extensiones. {@link org.w3c.dom.css.ViewCSS} es la de CSS, y
 * agrega el metodo que de verdad se usa: pedirle el estilo **computado** de un elemento, que
 * depende de la vista porque un `em` mide distinto en pantalla que en papel.
 */
public interface AbstractView {

    /** El documento del que esta es una vista. */
    DocumentView getDocument();
}
