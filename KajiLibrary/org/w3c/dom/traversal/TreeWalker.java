package org.w3c.dom.traversal;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.traversal.TreeWalker -- recorre un documento <b>como arbol</b>.
 *
 * <p>Es la otra mitad de {@code org.w3c.dom.traversal}. Donde {@link NodeIterator} aplana el
 * documento a una lista, este conserva la forma: tiene {@link #parentNode()}, {@link #firstChild()}
 * y hermanos, y presenta un arbol <b>podado</b> -- el que queda despues de aplicar la mascara y el
 * filtro.
 *
 * <h2>El arbol que se ve no es el que hay</h2>
 *
 * <p>Y ahi esta lo que sorprende: si un nodo intermedio da {@code FILTER_SKIP}, sus hijos se
 * <b>promueven</b>, asi que {@link #parentNode()} desde uno de ellos devuelve el <b>abuelo</b>. El
 * recorrido es consistente consigo mismo, pero no coincide con {@code Node.getParentNode()} del
 * documento. Quien mezcle los dos se pierde.
 *
 * <p>Es tambien donde {@code FILTER_REJECT} se distingue de {@code FILTER_SKIP}: aca rechazar poda
 * el subarbol entero, mientras que en un iterador las dos hacen lo mismo.
 *
 * <h2>El nodo actual puede estar fuera de la vista</h2>
 *
 * <p>{@link #setCurrentNode} acepta <b>cualquier</b> nodo, incluso uno que el filtro esconde e
 * incluso uno fuera de la raiz. No es un descuido de la especificacion: sirve para reposicionar el
 * recorrido desde un nodo que se obtuvo por otro camino. Los movimientos posteriores si respetan el
 * filtro, asi que desde un nodo escondido se sale a la primera.
 */
public interface TreeWalker {

    /** La raiz del recorrido. Ningun movimiento sale de su subarbol. */
    Node getRoot();

    /** La mascara de tipos, un OR de las {@code SHOW_*} de {@link NodeFilter}. */
    int getWhatToShow();

    /** El filtro, o null si no hay. */
    NodeFilter getFilter();

    /** Si las referencias a entidad se expanden al recorrer. */
    boolean getExpandEntityReferences();

    /** Donde esta parado. Puede ser un nodo que el filtro esconde; ver la nota de la clase. */
    Node getCurrentNode();

    /**
     * Lo reposiciona.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} si es null
     */
    void setCurrentNode(Node currentNode) throws DOMException;

    /**
     * Sube al padre <b>visible</b>, o null si no hay ninguno adentro de la raiz.
     *
     * <p>Puede no ser el padre real; ver la nota de la clase.
     */
    Node parentNode();

    /** El primer hijo visible, o null. */
    Node firstChild();

    /** El ultimo hijo visible, o null. */
    Node lastChild();

    /** El hermano anterior visible, o null. */
    Node previousSibling();

    /** El hermano siguiente visible, o null. */
    Node nextSibling();

    /** El anterior en orden de documento dentro del arbol podado, o null. */
    Node previousNode();

    /** El siguiente en orden de documento dentro del arbol podado, o null. */
    Node nextNode();
}
