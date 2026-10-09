package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Document -- the root of the tree and the factory of everything that
 * goes inside it.
 *
 * <p>It has two different roles that it is as well not to mix. As a **node** it is the root: its
 * only element child is the root element of the XML, which is reached through {@link
 * #getDocumentElement}, and careful with the difference --the document and the root element are not
 * the same node, and next to the root element there may hang comments, processing instructions and
 * the {@link DocumentType}. As a **factory** it is the only place new nodes come from: there are no
 * constructors in the DOM, everything comes out of a {@code createXxx}.
 *
 * <p>That the factory is the document and not a loose class is what holds up the rule half the
 * errors of the DOM hang from: each node belongs **to the document that created it**, and putting
 * into a tree a node manufactured by another document is {@code WRONG_DOCUMENT_ERR}. To cross it
 * there are two roads and they are different: {@link #importNode} leaves the original where it was
 * and brings a copy, {@link #adoptNode} takes it away from the other tree.
 *
 * <p>The interface is declared whole. The {@code createXxx} are declared as befits a contract:
 * there is no implementation here, and declaring the signature does not promise a tree.
 */
public interface Document extends Node {

    /** The {@code <!DOCTYPE>}, or {@code null} if there is none. */
    public DocumentType getDoctype();

    /** The object that handles the questions about what this implementation supports. */
    public DOMImplementation getImplementation();

    /** The root element. It is not the document: it is its child. */
    public Element getDocumentElement();

    /**
     * A new element, with no namespace.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR} if the name is malformed
     */
    public Element createElement(String tagName) throws DOMException;

    /** An empty fragment, for gathering nodes before inserting them at once. */
    public DocumentFragment createDocumentFragment();

    /** A text node with that content. */
    public Text createTextNode(String data);

    /** A comment with that content. */
    public Comment createComment(String data);

    /**
     * A CDATA section with that content.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} in an HTML document
     */
    public CDATASection createCDATASection(String data) throws DOMException;

    /**
     * A processing instruction.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR} or {@code NOT_SUPPORTED_ERR}
     */
    public ProcessingInstruction createProcessingInstruction(String target, String data)
            throws DOMException;

    /**
     * A loose attribute, with no element. To attach it, {@link Element#setAttributeNode} has to be
     * used.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR}
     */
    public Attr createAttribute(String name) throws DOMException;

    /**
     * An unexpanded entity reference.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR} or {@code NOT_SUPPORTED_ERR}
     */
    public EntityReference createEntityReference(String name) throws DOMException;

    /** The elements of the document with that tag, in order. The list is live. */
    public NodeList getElementsByTagName(String tagname);

    /**
     * A copy of the node, created by **this** document, ready to insert; the original is not
     * touched. With {@code deep} at {@code false} it comes with no children.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} if the type of node cannot be imported
     */
    public Node importNode(Node importedNode, boolean deep) throws DOMException;

    /**
     * A new element with a namespace.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR}, {@code NAMESPACE_ERR} or
     *     {@code NOT_SUPPORTED_ERR}
     */
    public Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException;

    /**
     * A new attribute with a namespace.
     *
     * @throws DOMException {@code INVALID_CHARACTER_ERR}, {@code NAMESPACE_ERR} or
     *     {@code NOT_SUPPORTED_ERR}
     */
    public Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException;

    /** The elements with that namespace and local name, in order. */
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName);

    /**
     * The element whose ID-type attribute has that value, or {@code null}.
     *
     * <p>It depends on **something** having declared that attribute as an ID: the DTD, a schema, or
     * {@link Element#setIdAttribute}. Without that it finds nothing, even if the attribute is
     * called {@code "id"}.
     */
    public Element getElementById(String elementId);

    /**
     * The encoding detected when parsing, or {@code null} if the document did not come from a
     * parser.
     */
    public String getInputEncoding();

    /** The encoding declared in the XML declaration, or {@code null}. */
    public String getXmlEncoding();

    /** What {@code standalone} said --or says-- in the XML declaration. */
    public boolean getXmlStandalone();

    /**
     * It sets {@code standalone}.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} in a document that does not support XML
     */
    public void setXmlStandalone(boolean xmlStandalone) throws DOMException;

    /** The XML version, typically {@code "1.0"} or {@code "1.1"}. */
    public String getXmlVersion();

    /**
     * It changes the XML version.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} if the version is not supported
     */
    public void setXmlVersion(String xmlVersion) throws DOMException;

    /**
     * Whether errors are checked on each operation.
     *
     * <p>Switching it off is an escape valve for building large trees quickly, in exchange for the
     * document possibly being left invalid without anybody warning.
     */
    public boolean getStrictErrorChecking();

    /** It switches the checking of errors on or off. */
    public void setStrictErrorChecking(boolean strictErrorChecking);

    /** The URI of the document, or {@code null} if it is not known. */
    public String getDocumentURI();

    /** It sets the URI of the document. */
    public void setDocumentURI(String documentURI);

    /**
     * It takes the node from its previous document to this one, **moving** it: the original is left
     * without that subtree.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} or {@code NO_MODIFICATION_ALLOWED_ERR}
     */
    public Node adoptNode(Node source) throws DOMException;

    /** The configuration {@link #normalizeDocument} uses. */
    public DOMConfiguration getDomConfig();

    /**
     * It leaves the document as if it had been serialised and parsed again, applying whatever
     * {@link #getDomConfig} says: it joins texts, resolves namespaces, and validates if asked to.
     */
    public void normalizeDocument();

    /**
     * It changes the name --and the namespace-- of an {@link Element} or an {@link Attr}, returning
     * the renamed node, which may be another object.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR}, {@code INVALID_CHARACTER_ERR},
     *     {@code WRONG_DOCUMENT_ERR} or {@code NAMESPACE_ERR}
     */
    public Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException;
}
