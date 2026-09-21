package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.NamedNodeMap -- nodes accessible by name, not by position.
 *
 * <p>It is what `Node.getAttributes()` returns and what the entities and notations of a
 * `DocumentType` keep. What it adds over `NodeList` is the access by name; what it does **not** add
 * is order: the specification says explicitly that `item(int)` is there to be able to walk it whole
 * and that the order means nothing. Code that depends on it breaks on changing implementation.
 *
 * <p>The `xxxNS` pairs of methods are the namespace model, which lives with the other without
 * mixing: an attribute set with `setNamedItem` is looked up with `getNamedItem` by its complete
 * qualified name, and one set with `setNamedItemNS` is looked up by (URI, local name).
 */
public interface NamedNodeMap {

    Node getNamedItem(String name);

    /**
     * It returns the node that was there with that name, or `null`.
     *
     * @throws DOMException with `INUSE_ATTRIBUTE_ERR` if the `Attr` already belongs to another
     *         element: an attribute is not shared, it is cloned.
     */
    Node setNamedItem(Node arg) throws DOMException;

    /**
     * @throws DOMException with `NOT_FOUND_ERR` if there is none with that name. Here the DOM does
     *         throw, unlike `getNamedItem`, which returns `null`.
     */
    Node removeNamedItem(String name) throws DOMException;

    /** `null` if the index is out of range. The order is not defined. */
    Node item(int index);

    int getLength();

    Node getNamedItemNS(String namespaceURI, String localName) throws DOMException;

    Node setNamedItemNS(Node arg) throws DOMException;

    Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException;
}
