package org.w3c.dom.ls;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.ls.LSParserFilter -- decide que entra al arbol, mientras se analiza.
 *
 * <p>Para lo que sirve de verdad es para <b>no construir</b> lo que no interesa. Un documento de
 * cien megas del que solo importan unos elementos se puede analizar sin que el arbol entero llegue a
 * existir, y esa es la unica forma de leerlo con DOM sin quedarse sin memoria.
 *
 * <h2>Los dos metodos son dos momentos</h2>
 *
 * <p>{@link #startElement} se llama al ver la etiqueta de apertura, con el elemento <b>vacio</b>:
 * todavia no tiene hijos ni texto. {@link #acceptNode} se llama con el nodo ya completo. La
 * diferencia es todo el punto: rechazar en {@code startElement} evita construir el subarbol, y
 * rechazar en {@code acceptNode} solo lo tira despues de haberlo armado. Solo se puede decidir
 * temprano con lo que hay en la etiqueta --el nombre y los atributos-- y por eso ahi conviene mirar.
 *
 * <p>{@link #SKIP} y {@link #REJECT} tampoco son lo mismo: saltear descarta el elemento pero
 * <b>conserva</b> sus hijos, que suben un nivel; rechazar se lleva el subarbol entero.
 *
 * <p>{@link #getWhatToShow} limita a que tipos de nodo se le pregunta, con las mascaras de
 * {@code NodeFilter}. Sirve para no pagar una llamada por cada nodo de texto de un documento cuando
 * el filtro solo mira elementos.
 */
public interface LSParserFilter {

    /** El nodo entra tal cual. */
    short FILTER_ACCEPT = 1;

    /** El nodo y todo su subarbol se descartan. */
    short FILTER_REJECT = 2;

    /** El nodo se descarta pero sus hijos suben un nivel. Ver la nota de la clase. */
    short FILTER_SKIP = 3;

    /** Se corta el analisis; el documento queda incompleto. */
    short FILTER_INTERRUPT = 4;

    /**
     * Al abrir la etiqueta, con el elemento todavia vacio.
     *
     * <p>Es el momento en que conviene rechazar; ver la nota de la clase.
     *
     * @return una de las cuatro constantes
     */
    short startElement(Element elementArg);

    /**
     * Con el nodo ya armado.
     *
     * @return una de las cuatro constantes; {@link #FILTER_SKIP} no vale para elementos que ya se
     *     aceptaron en {@link #startElement}
     */
    short acceptNode(Node nodeArg);

    /** Que tipos de nodo se le pasan, con las mascaras de {@code NodeFilter}. */
    int getWhatToShow();
}
