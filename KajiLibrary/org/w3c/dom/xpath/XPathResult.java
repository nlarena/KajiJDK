package org.w3c.dom.xpath;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.xpath.XPathResult -- lo que devolvio una expresion.
 *
 * <p>Un objeto con diez tipos posibles y un accesor por tipo: leer el equivocado lanza. Se pregunta
 * primero con {@link #getResultType()}, salvo que se haya pedido un tipo concreto al evaluar --y ahi
 * ya se sabe cual es--.
 *
 * <h2>Iterador o instantanea: la eleccion que importa</h2>
 *
 * <p>Los cuatro tipos de conjunto de nodos se dividen en dos pares, y la diferencia no es de estilo:
 *
 * <ul>
 *   <li><b>iterator</b> -- perezoso. Si el documento cambia mientras se recorre, el iterador se
 *       <b>invalida</b> y {@link #iterateNext()} lanza. Se pregunta con
 *       {@link #getInvalidIteratorState()}.
 *   <li><b>snapshot</b> -- se materializa al evaluar. Sobrevive a los cambios, pero los nodos que
 *       tiene adentro pueden haber dejado de estar en el documento.
 * </ul>
 *
 * <p>Y lo de "ordered" tambien cuesta: pedir orden de documento puede ser mas caro, asi que la
 * version desordenada existe para cuando no importa el orden.
 */
public interface XPathResult {

    /** Lo que la expresion de naturalmente. */
    short ANY_TYPE = 0;

    /** Un numero. */
    short NUMBER_TYPE = 1;

    /** Una cadena. */
    short STRING_TYPE = 2;

    /** Un booleano. */
    short BOOLEAN_TYPE = 3;

    /** Conjunto de nodos perezoso, sin orden garantizado. */
    short UNORDERED_NODE_ITERATOR_TYPE = 4;

    /** Conjunto de nodos perezoso, en orden de documento. */
    short ORDERED_NODE_ITERATOR_TYPE = 5;

    /** Conjunto de nodos materializado, sin orden garantizado. */
    short UNORDERED_NODE_SNAPSHOT_TYPE = 6;

    /** Conjunto de nodos materializado, en orden de documento. */
    short ORDERED_NODE_SNAPSHOT_TYPE = 7;

    /** Un solo nodo cualquiera del conjunto. */
    short ANY_UNORDERED_NODE_TYPE = 8;

    /** El primer nodo en orden de documento. */
    short FIRST_ORDERED_NODE_TYPE = 9;

    /** Cual de los diez es. */
    short getResultType();

    /** @throws XPathException {@code TYPE_ERR} si el resultado no es un numero */
    double getNumberValue() throws XPathException;

    /** @throws XPathException {@code TYPE_ERR} si el resultado no es una cadena */
    String getStringValue() throws XPathException;

    /** @throws XPathException {@code TYPE_ERR} si el resultado no es un booleano */
    boolean getBooleanValue() throws XPathException;

    /** @throws XPathException {@code TYPE_ERR} si el resultado no es un nodo suelto */
    Node getSingleNodeValue() throws XPathException;

    /** Si el documento cambio y el iterador dejo de servir. Solo aplica a los tipos perezosos. */
    boolean getInvalidIteratorState();

    /** @throws XPathException {@code TYPE_ERR} si el resultado no es una instantanea */
    int getSnapshotLength() throws XPathException;

    /**
     * El siguiente nodo, o null si se acabo.
     *
     * @throws XPathException {@code TYPE_ERR} si no es un iterador
     * @throws DOMException {@code INVALID_STATE_ERR} si el documento cambio; ver la nota de la clase
     */
    Node iterateNext() throws XPathException, DOMException;

    /**
     * El nodo en esa posicion de la instantanea, o null si el indice se pasa.
     *
     * @throws XPathException {@code TYPE_ERR} si no es una instantanea
     */
    Node snapshotItem(int index) throws XPathException;
}
