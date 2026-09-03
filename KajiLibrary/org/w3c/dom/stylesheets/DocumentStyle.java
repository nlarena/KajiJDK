package org.w3c.dom.stylesheets;

/**
 * Un documento que expone sus hojas de estilo.
 *
 * <p>Es una interfaz aparte y no un metodo de `Document` porque un documento XML sin hojas de
 * estilo no tiene por que implementarla: el DOM se arma por capas y esta es la de estilos.
 */
public interface DocumentStyle {

    /** Las hojas del documento, en una lista viva. */
    StyleSheetList getStyleSheets();
}
