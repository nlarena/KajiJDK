package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Node -- the base type of everything that lives in a DOM tree.
 *
 * <p><strong>The model, once.</strong> The DOM represents an XML document as a tree of **nodes**.
 * An element is a node, an attribute is a node, loose text is a node, a comment is a node, and the
 * whole document as well. That is why `Node` is above almost all of the package: it is the lowest
 * common denominator that allows writing a generic walk --going down through `getFirstChild()`,
 * moving on through `getNextSibling()`-- without knowing what there is on each step. The price of
 * that common minimum is that the interface **declares more than any concrete node fulfils**, and
 * that is the key for reading the whole rest of this package.
 *
 * <p><strong>Why the DOM is full of methods that return `null`.</strong> `getAttributes()` only
 * makes sense on an `Element`; on a `Text` it returns `null`. `getOwnerDocument()` returns `null`
 * precisely on the `Document`. `getNamespaceURI()`, `getPrefix()` and `getLocalName()` return
 * `null` on every node created with the DOM Level 1 API, which did not know namespaces. They are
 * not holes in the specification: they are the consequence of having chosen **one** interface for
 * twelve node types instead of twelve interfaces with no common ancestor. Whoever walks a DOM tree
 * checks `getNodeType()` before believing a getter.
 *
 * <p><strong>The twelve types.</strong> The `*_NODE` constants are the discriminator:
 * `getNodeType()` returns one of them and from there follows which child interface one can cast to.
 * The table of the standard also fixes what `getNodeName()` and `getNodeValue()` return for each
 * one --`#text` and the contents for a `Text`, the name of the tag and `null` for an `Element`--
 * and that table is part of the contract even though it cannot be written in Java.
 *
 * <p><strong>The `DOCUMENT_POSITION_*` constants are a bit mask</strong>, not an enum:
 * `compareDocumentPosition` returns the **or** of all that apply. That is why they are worth 1, 2,
 * 4, 8, 16 and 32 and not 1..6. A node compared with one of its ancestors gets `CONTAINS |
 * PRECEDING` --the other node contains it and comes before it--; the note said that "a node that
 * contains another returns" it, which is the other way round (the ancestor gets `CONTAINED_BY |
 * FOLLOWING`).
 *
 * <p><strong>This is an interface.</strong> Declaring the contract is honest precisely because it
 * is a contract: it does not promise that anybody fulfils it. The note said there is no
 * implementation in KajiLibrary; there is a partial one, as in the JDK: {@code
 * javax.imageio.metadata.IIOMetadataNode} implements {@link Element} for image metadata trees. What
 * there is not, and could not honestly be given, is a fake {@code Document} that pretends to parse
 * XML. The methods that build or modify trees --`appendChild`, `cloneNode`, `normalize`-- are
 * declared because they are part of the interface an implementer has to fulfil.
 */
public interface Node {

    // ---- the twelve node types ------------------------------------------------------------------
    //
    // The order and the values are those of the standard and are observable API: there is code that
    // keeps them in tables indexed by the number. `getNodeType()` returns one of these.

    /** An `Element`: a tag with attributes and children. */
    short ELEMENT_NODE = 1;

    /**
     * An `Attr`. It hangs from its element, but it is **not** its child: `getParentNode()` gives
     * `null`.
     */
    short ATTRIBUTE_NODE = 2;

    /** A `Text`: loose characters between tags. */
    short TEXT_NODE = 3;

    /** A `CDATASection`: text the parser does not interpret. */
    short CDATA_SECTION_NODE = 4;

    /** An `EntityReference`: an unexpanded `&amp;name;`. */
    short ENTITY_REFERENCE_NODE = 5;

    /** An `Entity` declared in the DTD. */
    short ENTITY_NODE = 6;

    /** A `ProcessingInstruction`: `&lt;?target data?&gt;`. */
    short PROCESSING_INSTRUCTION_NODE = 7;

    /** A `Comment`. */
    short COMMENT_NODE = 8;

    /** The `Document`: the root of the tree, which is not the root element. */
    short DOCUMENT_NODE = 9;

    /** The `DocumentType`: the `&lt;!DOCTYPE ...&gt;`. */
    short DOCUMENT_TYPE_NODE = 10;

    /** A `DocumentFragment`: a lightweight container for moving several nodes at once. */
    short DOCUMENT_FRAGMENT_NODE = 11;

    /** A `Notation` declared in the DTD. */
    short NOTATION_NODE = 12;

    // ---- relative position: a bit mask, not exclusive values -----------------------------------

    /** The two nodes are not in the same tree. */
    short DOCUMENT_POSITION_DISCONNECTED = 0x01;

    /** The other node comes **before** this one. */
    short DOCUMENT_POSITION_PRECEDING = 0x02;

    /** The other node comes **after** this one. */
    short DOCUMENT_POSITION_FOLLOWING = 0x04;

    /** The other node is an ancestor of this one. */
    short DOCUMENT_POSITION_CONTAINS = 0x08;

    /** The other node is a descendant of this one. */
    short DOCUMENT_POSITION_CONTAINED_BY = 0x10;

    /** The order between the two is chosen by the implementation and may change between runs. */
    short DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC = 0x20;

    // ---- identity of the node --------------------------------------------------------------------

    String getNodeName();

    /**
     * The value, for the nodes that have one; `null` for `Element`, `Document` and company.
     *
     * @throws DOMException with `DOMSTRING_SIZE_ERR` if the value does not fit in a `String`. It is
     *         the leftover of an era in which a `DOMString` could be larger than what was
     *         addressable.
     */
    String getNodeValue() throws DOMException;

    /**
     * @throws DOMException with `NO_MODIFICATION_ALLOWED_ERR` if the node is read-only, which
     *         happens with everything that hangs from an `Entity` or from an `EntityReference`.
     */
    void setNodeValue(String nodeValue) throws DOMException;

    short getNodeType();

    // ---- navigation ------------------------------------------------------------------------------
    //
    // The five getters below return `null` when there is nowhere to go. It is the criterion of the
    // whole DOM: never an exception for "there is none", always `null`.

    Node getParentNode();

    /** Never `null`: a node with no children returns an empty list, not `null`. */
    NodeList getChildNodes();

    Node getFirstChild();

    Node getLastChild();

    Node getPreviousSibling();

    Node getNextSibling();

    /** Only an `Element` returns something; the rest, `null`. */
    NamedNodeMap getAttributes();

    /** The document that **created** this node. `null` on the `Document` itself. */
    Document getOwnerDocument();

    // ---- modifying the tree ----------------------------------------------------------------------
    //
    // The insertions **move**, they do not copy: inserting a node that already has a parent takes
    // it out of where it was. And if what is inserted is a `DocumentFragment`, what goes in are its
    // children and not the fragment.

    /**
     * @param refChild if it is `null`, it is equivalent to `appendChild`.
     * @throws DOMException with `HIERARCHY_REQUEST_ERR` if the type of child does not go there or
     *         if it would create a cycle; `WRONG_DOCUMENT_ERR` if it comes from another document;
     *         `NOT_FOUND_ERR` if `refChild` is not a child of this node.
     */
    Node insertBefore(Node newChild, Node refChild) throws DOMException;

    /** It returns the node **removed**, not the one put in. */
    Node replaceChild(Node newChild, Node oldChild) throws DOMException;

    Node removeChild(Node oldChild) throws DOMException;

    Node appendChild(Node newChild) throws DOMException;

    boolean hasChildNodes();

    /**
     * @param deep if it is `false`, the clone has no children. An `Element` clones its attributes
     *        all the same: `deep` speaks of the children, not of the attributes.
     */
    Node cloneNode(boolean deep);

    /**
     * It joins adjacent `Text`s into one and throws away the empty ones.
     *
     * <p>It matters because a freshly parsed tree may have the same paragraph split into several
     * `Text`s --for example if there was an entity reference in the middle-- and that breaks any
     * naive comparison. After `normalize()` the shape of the tree is the one that would be obtained
     * by serialising and parsing again.
     */
    void normalize();

    /**
     * `getFeature` replaced it in Level 3, which besides saying whether it is there returns the
     * object.
     */
    boolean isSupported(String feature, String version);

    // ---- namespaces ------------------------------------------------------------------------------
    //
    // The three return `null` on every node created with the Level 1 API (`createElement` instead
    // of `createElementNS`). It is not that the node has no namespace: it is that it **takes no
    // part** in the namespace model, which is different.

    String getNamespaceURI();

    String getPrefix();

    /**
     * @throws DOMException with `NAMESPACE_ERR` if the prefix is malformed, or if an attempt is
     *         made to bind `xml` or `xmlns` to a URI that is not their own.
     */
    void setPrefix(String prefix) throws DOMException;

    String getLocalName();

    boolean hasAttributes();

    // ---- additions of Level 3 --------------------------------------------------------------------

    /** The base URI for resolving relative references, following `xml:base`. */
    String getBaseURI();

    /** An **or** of the `DOCUMENT_POSITION_*` constants. */
    short compareDocumentPosition(Node other) throws DOMException;

    /** All the text below concatenated, with no markup. */
    String getTextContent() throws DOMException;

    /** It replaces all the children by a single `Text`; with `null` or `""` it deletes them all. */
    void setTextContent(String textContent) throws DOMException;

    /**
     * Identity, not equality. It exists because an implementation may hand over more than one Java
     * object for the same node of the document, and then `==` is not enough.
     */
    boolean isSameNode(Node other);

    String lookupPrefix(String namespaceURI);

    boolean isDefaultNamespace(String namespaceURI);

    String lookupNamespaceURI(String prefix);

    /** Structural equality: same type, same name, same attributes, same children in order. */
    boolean isEqualNode(Node arg);

    /**
     * The object that implements `feature` for this node, or `null`.
     *
     * <p>It returns `Object` and not something more precise because what comes out of here is
     * usually from **another** package --`org.w3c.dom.events.EventTarget`,
     * `org.w3c.dom.ls.LSSerializer`-- and the DOM core does not depend on its optional modules.
     */
    Object getFeature(String feature, String version);

    /** It returns whatever there was before with that key. */
    Object setUserData(String key, Object data, UserDataHandler handler);

    Object getUserData(String key);
}
