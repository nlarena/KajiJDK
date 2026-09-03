package org.w3c.dom.ranges;

import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.ranges.Range -- un tramo del documento, que puede empezar y terminar en
 * el medio de un texto.
 *
 * <p>Es lo que hace falta para representar una <b>seleccion</b>. Un {@code Node} no alcanza: cuando
 * alguien selecciona con el mouse, lo seleccionado empieza a la mitad de un parrafo y termina a la
 * mitad de otro, y no hay ningun nodo que sea eso.
 *
 * <h2>Un extremo es un par (contenedor, desplazamiento)</h2>
 *
 * <p>Y el desplazamiento significa dos cosas distintas segun el contenedor, que es lo que confunde:
 *
 * <ul>
 *   <li>Si el contenedor es un nodo de <b>texto</b>, es un indice de <b>caracter</b>.
 *   <li>Si es cualquier otro, es un indice de <b>hijo</b>: cuantos hijos quedan antes del extremo.
 * </ul>
 *
 * <p>De ahi sale que un desplazamiento valido llegue hasta {@code length} inclusive y no hasta
 * {@code length - 1}: el extremo puede estar <b>despues</b> del ultimo caracter o del ultimo hijo.
 *
 * <h2>Extraer no es clonar</h2>
 *
 * <p>Los tres metodos que trabajan sobre el contenido se parecen y hacen cosas distintas:
 * {@link #cloneContents()} copia y no toca el documento, {@link #extractContents()} <b>lo saca</b> y
 * lo devuelve, y {@link #deleteContents()} lo saca y no devuelve nada. Los dos ultimos dejan el
 * rango colapsado donde estaba el contenido.
 *
 * <p>Un rango <b>sigue vivo</b> cuando el documento cambia: se ajusta, igual que un
 * {@link org.w3c.dom.traversal.NodeIterator}. Por eso tambien tiene {@link #detach()}.
 */
public interface Range {

    /** Compara el principio de este rango con el principio del otro. */
    short START_TO_START = 0;

    /** Compara el <b>final</b> de este rango con el <b>principio</b> del otro. */
    short START_TO_END = 1;

    /** Compara el final de este con el final del otro. */
    short END_TO_END = 2;

    /** Compara el <b>principio</b> de este con el <b>final</b> del otro. */
    short END_TO_START = 3;

    /** El nodo donde empieza. */
    Node getStartContainer() throws DOMException;

    /** Donde empieza adentro de el. Ver la nota de la clase sobre que significa. */
    int getStartOffset() throws DOMException;

    /** El nodo donde termina. */
    Node getEndContainer() throws DOMException;

    /** Donde termina adentro de el. */
    int getEndOffset() throws DOMException;

    /** Si los dos extremos coinciden, o sea si el rango esta vacio. */
    boolean getCollapsed() throws DOMException;

    /** El antepasado comun mas cercano de los dos extremos. */
    Node getCommonAncestorContainer() throws DOMException;

    /**
     * Mueve el principio.
     *
     * <p>Si el nuevo principio queda <b>despues</b> del final, el rango se colapsa ahi en vez de
     * quedar invertido. Es del estandar y evita que exista un rango imposible.
     *
     * @throws RangeException {@code INVALID_NODE_TYPE_ERR} si ese nodo no puede contener un extremo
     * @throws DOMException {@code INDEX_SIZE_ERR} si el desplazamiento se pasa
     */
    void setStart(Node refNode, int offset) throws RangeException, DOMException;

    /** Mueve el final. Vale lo mismo que para {@link #setStart}, al reves. */
    void setEnd(Node refNode, int offset) throws RangeException, DOMException;

    /** Pone el principio justo antes de ese nodo. */
    void setStartBefore(Node refNode) throws RangeException, DOMException;

    /** Pone el principio justo despues de ese nodo. */
    void setStartAfter(Node refNode) throws RangeException, DOMException;

    /** Pone el final justo antes de ese nodo. */
    void setEndBefore(Node refNode) throws RangeException, DOMException;

    /** Pone el final justo despues de ese nodo. */
    void setEndAfter(Node refNode) throws RangeException, DOMException;

    /**
     * Junta los dos extremos.
     *
     * @param toStart si se colapsa al principio; si es false, al final
     */
    void collapse(boolean toStart) throws DOMException;

    /** Hace que el rango sea exactamente ese nodo, el nodo mismo incluido. */
    void selectNode(Node refNode) throws RangeException, DOMException;

    /** Hace que el rango sea el <b>contenido</b> de ese nodo, sin el nodo. */
    void selectNodeContents(Node refNode) throws RangeException, DOMException;

    /**
     * Compara un extremo de este rango con uno del otro.
     *
     * @param how cual con cual: una de las cuatro constantes. Ojo con {@link #START_TO_END} y
     *     {@link #END_TO_START}, que cruzan los extremos
     * @return -1, 0 o 1
     */
    short compareBoundaryPoints(short how, Range sourceRange) throws DOMException;

    /** Borra el contenido del documento. El rango queda colapsado ahi. */
    void deleteContents() throws DOMException;

    /** Lo <b>saca</b> del documento y lo devuelve. Ver la nota de la clase. */
    DocumentFragment extractContents() throws DOMException;

    /** Lo <b>copia</b> sin tocar el documento. */
    DocumentFragment cloneContents() throws DOMException;

    /**
     * Mete ese nodo en el principio del rango.
     *
     * <p>Si el principio esta a la mitad de un texto, el texto se <b>parte</b> para hacerle lugar.
     */
    void insertNode(Node newNode) throws DOMException, RangeException;

    /**
     * Envuelve el contenido del rango con ese nodo.
     *
     * @throws RangeException {@code BAD_BOUNDARYPOINTS_ERR} si el rango parte un nodo por la mitad:
     *     envolver algo que empieza adentro de un elemento y termina afuera daria un arbol imposible
     */
    void surroundContents(Node newParent) throws DOMException, RangeException;

    /** Una copia independiente de este rango. */
    Range cloneRange() throws DOMException;

    /** El texto del contenido, sin marcas. */
    String toString();

    /** Suelta el rango: el documento deja de tener que ajustarlo. Despues, todo lo demas lanza. */
    void detach() throws DOMException;
}
