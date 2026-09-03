package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.ElementTraversal -- recorrer el arbol viendo **solo** elementos.
 *
 * <p>Es la mas joven del paquete y la unica que no viene del DOM Core: es una recomendacion aparte
 * del W3C, "Element Traversal". Responde a una molestia concreta de todos los dias: en un XML con
 * sangria, entre dos elementos hermanos hay un nodo {@link Text} de espacios, asi que
 * {@link Node#getFirstChild} casi nunca devuelve el elemento que uno queria y todo el mundo termina
 * escribiendo el mismo bucle de saltear texto y comentarios.
 *
 * <p>Notar que **no** extiende {@link Node} ni {@link Element}: es una interfaz suelta que una
 * implementacion le agrega a sus nodos elemento. Por eso puede aparecer en un {@code instanceof}
 * sobre algo que ya se sabe que es un {@link Element}, y por eso no todo {@code Element} la tiene.
 *
 * <p>Los cinco metodos son la vista filtrada de los cinco de navegacion de {@link Node}, y
 * {@link #getChildElementCount} es lo que seria {@code getChildNodes().getLength()} contando solo
 * elementos.
 *
 * <p>Interfaz declarada entera.
 */
public interface ElementTraversal {

    /** El primer hijo que sea elemento, o {@code null}. */
    public Element getFirstElementChild();

    /** El ultimo hijo que sea elemento, o {@code null}. */
    public Element getLastElementChild();

    /** El hermano anterior que sea elemento, o {@code null}. */
    public Element getPreviousElementSibling();

    /** El hermano siguiente que sea elemento, o {@code null}. */
    public Element getNextElementSibling();

    /** Cuantos hijos son elementos. */
    public int getChildElementCount();
}
