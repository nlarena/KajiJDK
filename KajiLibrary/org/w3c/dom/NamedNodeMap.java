package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.NamedNodeMap -- nodos accesibles por nombre, no por posicion.
 *
 * <p>Es lo que devuelve `Node.getAttributes()` y lo que guardan las entidades y notaciones de un
 * `DocumentType`. Lo que agrega sobre `NodeList` es el acceso por nombre; lo que **no** agrega es
 * orden: la especificacion dice explicitamente que `item(int)` esta para poder recorrerla entera y
 * que el orden no significa nada. Codigo que dependa de el se rompe al cambiar de implementacion.
 *
 * <p>Los pares de metodos `xxxNS` son el modelo de espacios de nombres, que convive con el otro sin
 * mezclarse: un atributo puesto con `setNamedItem` se busca con `getNamedItem` por su nombre
 * calificado completo, y uno puesto con `setNamedItemNS` se busca por (URI, nombre local).
 */
public interface NamedNodeMap {

    Node getNamedItem(String name);

    /**
     * Devuelve el nodo que estaba con ese nombre, o `null`.
     *
     * @throws DOMException con `INUSE_ATTRIBUTE_ERR` si el `Attr` ya pertenece a otro elemento: un
     *         atributo no se comparte, se clona.
     */
    Node setNamedItem(Node arg) throws DOMException;

    /**
     * @throws DOMException con `NOT_FOUND_ERR` si no hay ninguno con ese nombre. Aca el DOM si tira,
     *         a diferencia de `getNamedItem`, que devuelve `null`.
     */
    Node removeNamedItem(String name) throws DOMException;

    /** `null` si el indice esta fuera de rango. El orden no esta definido. */
    Node item(int index);

    int getLength();

    Node getNamedItemNS(String namespaceURI, String localName) throws DOMException;

    Node setNamedItemNS(Node arg) throws DOMException;

    Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException;
}
