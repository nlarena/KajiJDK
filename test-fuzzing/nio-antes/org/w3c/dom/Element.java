package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Element -- a tag of the document.
 *
 * <p>It is the only node type with attributes, and by far the one that adds the most methods of its
 * own. Almost all come in pairs --{@code getAttribute} / {@code getAttributeNS}-- because DOM Level
 * 1 was written before namespaces existed and Level 2 could not change the old signatures without
 * breaking everything. The two families live together over the **same** set of attributes and
 * seeing them as two different collections is the classic mistake.
 *
 * <p>The real difference: the version without {@code NS} indexes by the complete name as it is
 * written, prefix included, and the {@code NS} version by the pair (URI, local name). For a
 * document with no namespaces they are the same; for one with namespaces, {@code
 * getAttribute("x:id")} finds what {@code getAttributeNS(uri, "id")} also finds, but {@code
 * getAttribute("id")} finds nothing.
 *
 * <p>Another pair worth telling apart: {@link #getAttribute} returns the **value** --and returns
 * {@code ""} both if the attribute is empty and if it does not exist, hence {@link #hasAttribute}
 * exists-- while {@link #getAttributeNode} returns the **node**, or {@code null}.
 *
 * <p>The three {@code setIdAttribute*} are from DOM Level 3 and do something that sounds odd: they
 * mark an attribute as of type ID **after** the tree has been built, so that
 * {@link Document#getElementById} finds it without there having been a DTD or a schema.
 *
 * <p>The interface is declared whole.
 */
public interface Element extends Node {

    /** The name of the tag, the same as {@link Node#getNodeName}. */
    public String getTagName();

    /**
     * The value of the attribute, or {@code ""} if it does not exist --which cannot be told from an
     * empty value.
     */
    public String getAttribute(String name);

    /**
     * It sets or replaces an attribute. The value is taken literally, without parsing entities.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR} or {@code NO_MODIFICATION_ALLOWED_ERR}
     */
    public void setAttribute(String name, String value) throws DOMException;

    /**
     * It removes the attribute. If the DTD declared a default value for it, it reappears with that
     * value; it does not fail if the attribute was not there.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR}
     */
    public void removeAttribute(String name) throws DOMException;

    /** The node of the attribute, or {@code null}. */
    public Attr getAttributeNode(String name);

    /**
     * It adds the attribute node and returns the one it replaced, or {@code null}.
     *
     * @throws DOMException {@code WRONG_DOCUMENT_ERR}, {@code NO_MODIFICATION_ALLOWED_ERR} or
     *     {@code INUSE_ATTRIBUTE_ERR} if the attribute already belongs to another element
     */
    public Attr setAttributeNode(Attr newAttr) throws DOMException;

    /**
     * It removes that attribute node and returns it.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} or {@code NOT_FOUND_ERR}
     */
    public Attr removeAttributeNode(Attr oldAttr) throws DOMException;

    /**
     * The descendants with that tag, in document order; {@code "*"} brings them all. The list is
     * live.
     */
    public NodeList getElementsByTagName(String name);

    /**
     * The value of the attribute with that namespace and local name, or {@code ""}.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} if the implementation does not handle XML
     */
    public String getAttributeNS(String namespaceURI, String localName) throws DOMException;

    /**
     * It sets or replaces an attribute with a namespace.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR}, {@code NO_MODIFICATION_ALLOWED_ERR},
     *     {@code NAMESPACE_ERR} or {@code NOT_SUPPORTED_ERR}
     */
    public void setAttributeNS(String namespaceURI, String qualifiedName, String value)
            throws DOMException;

    /**
     * It removes the attribute with that namespace and local name.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} or {@code NOT_SUPPORTED_ERR}
     */
    public void removeAttributeNS(String namespaceURI, String localName) throws DOMException;

    /**
     * The node of the attribute with that namespace and local name, or {@code null}.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR}
     */
    public Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException;

    /**
     * It adds the attribute node by (URI, local name) and returns the one it replaced, or {@code
     * null}.
     *
     * @throws DOMException {@code WRONG_DOCUMENT_ERR}, {@code NO_MODIFICATION_ALLOWED_ERR},
     *     {@code INUSE_ATTRIBUTE_ERR} or {@code NOT_SUPPORTED_ERR}
     */
    public Attr setAttributeNodeNS(Attr newAttr) throws DOMException;

    /**
     * The descendants with that namespace and local name, in document order.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR}
     */
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName)
            throws DOMException;

    /** Whether the attribute exists, or whether the DTD gives it a default value. */
    public boolean hasAttribute(String name);

    /**
     * Whether the attribute with that namespace and local name exists.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR}
     */
    public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException;

    /** The type information of the schema for this element, or {@code null}. */
    public TypeInfo getSchemaTypeInfo();

    /**
     * It marks --or unmarks-- that attribute as of type ID.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} or {@code NOT_FOUND_ERR}
     */
    public void setIdAttribute(String name, boolean isId) throws DOMException;

    /**
     * The same as {@link #setIdAttribute} but identifying the attribute by (URI, local name).
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} or {@code NOT_FOUND_ERR}
     */
    public void setIdAttributeNS(String namespaceURI, String localName, boolean isId)
            throws DOMException;

    /**
     * The same as {@link #setIdAttribute} but passing the node of the attribute.
     *
     * @throws DOMException {@code NO_MODIFICATION_ALLOWED_ERR} or {@code NOT_FOUND_ERR}
     */
    public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException;
}
