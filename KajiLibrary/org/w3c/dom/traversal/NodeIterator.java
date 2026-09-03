package org.w3c.dom.traversal;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.traversal.NodeIterator -- recorre un documento como si fuera una lista.
 *
 * <p>Aplana el arbol a su orden de documento y va y viene por el. Es la mitad simple de
 * {@code org.w3c.dom.traversal}; la otra, {@link TreeWalker}, conserva la forma del arbol.
 *
 * <h2>La posicion esta entre dos nodos, no sobre uno</h2>
 *
 * <p>Es lo que hace que {@link #nextNode()} y {@link #previousNode()} se comporten como uno espera al
 * cambiar de direccion: la posicion es un <b>hueco</b> en la lista, asi que llamar a `nextNode` y
 * despues a `previousNode` devuelve <b>el mismo nodo</b>, no el anterior. Quien lo lea como un cursor
 * sobre un nodo se pierde uno cada vez que da vuelta.
 *
 * <h2>El iterador sigue vivo si el documento cambia</h2>
 *
 * <p>No lanza {@code ConcurrentModificationException}: se <b>ajusta</b>. Si alguien borra el nodo
 * donde estaba parado, el iterador se acomoda para que el recorrido siga teniendo sentido. Eso lo
 * hace util y caro a la vez, y por eso existe {@link #detach()}: hasta que se llame, el documento
 * tiene que seguir avisandole de cada cambio.
 */
public interface NodeIterator {

    /** La raiz del recorrido. */
    Node getRoot();

    /** La mascara de tipos, un OR de las {@code SHOW_*} de {@link NodeFilter}. */
    int getWhatToShow();

    /** El filtro, o null si no hay. */
    NodeFilter getFilter();

    /** Si las referencias a entidad se expanden al recorrer. */
    boolean getExpandEntityReferences();

    /**
     * El siguiente nodo visible, o null si se acabo.
     *
     * @throws DOMException {@code INVALID_STATE_ERR} si ya se llamo a {@link #detach()}
     */
    Node nextNode() throws DOMException;

    /**
     * El anterior, o null si se llego al principio. Ver la nota de la clase sobre que devuelve al
     * cambiar de direccion.
     *
     * @throws DOMException {@code INVALID_STATE_ERR} si ya se llamo a {@link #detach()}
     */
    Node previousNode() throws DOMException;

    /**
     * Suelta el iterador: el documento deja de tener que avisarle de los cambios.
     *
     * <p>Despues de esto los dos metodos de recorrido lanzan. Llamarlo dos veces no hace nada.
     */
    void detach();
}
