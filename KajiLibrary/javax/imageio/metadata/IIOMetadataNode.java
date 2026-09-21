package javax.imageio.metadata;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

/**
 * KajiLibrary's javax.imageio.metadata.IIOMetadataNode -- a node of an image metadata tree.
 *
 * <p>It implements {@link org.w3c.dom.Element} so that metadata can be walked with the usual DOM
 * tools, without forcing anyone to learn a new API.
 *
 * <p>But it is <b>not a full DOM</b>, and it is worth knowing where it falls short:
 *
 * <ul>
 *   <li>there are no namespaces. The {@code xxxNS} methods exist and behave like the ones that are
 *       not; {@link #getNamespaceURI} returns null;
 *   <li>there is no owner document: {@link #getOwnerDocument} returns null. A metadata tree floats
 *       on its own;
 *   <li>all of level 3 --schema types, user data, position comparison, text content-- throws
 *       {@link DOMException} with {@code NOT_SUPPORTED_ERR}.
 * </ul>
 *
 * <h2>The user object</h2>
 *
 * <p>{@link #getUserObject} is what makes this class useful compared with a real DOM. A metadata
 * tree sometimes needs to carry a piece of data that is not text --a quantization table, a colour
 * profile, a byte array-- and putting it in as a string would be absurd.
 *
 * <p>When there is a user object, the attributes and children are usually superfluous: the node
 * <b>is</b> that object.
 *
 * <h2>The node is its own child list</h2>
 *
 * <p>It also implements {@link NodeList}, and {@link #getChildNodes} returns itself. It is a JDK
 * implementation shortcut that shows: {@code n.getChildNodes() == n} is true.
 *
 * <h2>{@link #cloneNode} does not copy the attributes</h2>
 *
 * <p>Not even in deep mode. It copies the name, the user object and --if deep-- the children, and
 * nothing else. It was checked against the JDK 25 and that is how it is; it looks like a 1999
 * oversight that can no longer be fixed without breaking whoever depends on it.
 */
public class IIOMetadataNode implements Element, NodeList {

    /** What it is called. */
    private String nodeName;

    /** The text value, almost always null. */
    private String nodeValue;

    /** The attributes. */
    private final List<Node> attributes = new ArrayList<Node>();

    /** The children, in order. */
    private final List<Node> children = new ArrayList<Node>();

    /** Whose child it is, or null. */
    private Node parent;

    /** The data that is not text. See the class note. */
    private Object userObject;

    /** Without a name; the name stays null. */
    public IIOMetadataNode() {
        this.nodeName = null;
    }

    /** @param nodeName what it is called */
    public IIOMetadataNode(String nodeName) {
        this.nodeName = nodeName;
    }

    /** What it is called. */
    public String getNodeName() {
        return this.nodeName;
    }

    /** The text value, or null. */
    public String getNodeValue() {
        return this.nodeValue;
    }

    /** Changes it. */
    public void setNodeValue(String nodeValue) {
        this.nodeValue = nodeValue;
    }

    /** Always {@link Node#ELEMENT_NODE}. */
    public short getNodeType() {
        return ELEMENT_NODE;
    }

    /** Whose child it is, or null. */
    public Node getParentNode() {
        return this.parent;
    }

    /** Returns itself. See the class note. */
    public NodeList getChildNodes() {
        return this;
    }

    /** The first child, or null. */
    public Node getFirstChild() {
        if (this.children.isEmpty()) {
            return null;
        }
        return this.children.get(0);
    }

    /** The last one, or null. */
    public Node getLastChild() {
        if (this.children.isEmpty()) {
            return null;
        }
        return this.children.get(this.children.size() - 1);
    }

    /** The previous sibling, or null. */
    public Node getPreviousSibling() {
        return siblingAt(-1);
    }

    /** The next one, or null. */
    public Node getNextSibling() {
        return siblingAt(1);
    }

    /**
     * The attributes, read-only. See {@link IIONamedNodeMap}.
     *
     * <p>Here it is a copy taken now; the JDK wraps the node's own attribute list, so its map sees
     * later changes.
     */
    public NamedNodeMap getAttributes() {
        return new IIONamedNodeMap(new ArrayList<Node>(this.attributes));
    }

    /** Null: a metadata tree has no document. See the class note. */
    public Document getOwnerDocument() {
        return null;
    }

    /**
     * Inserts before that child; with {@code refChild} null, appends at the end.
     *
     * @throws IllegalArgumentException if the new child is null
     * @throws DOMException if the reference one is not a child of this node
     */
    public Node insertBefore(Node newChild, Node refChild) {
        if (newChild == null) {
            throw new IllegalArgumentException("newChild == null!");
        }
        detach(newChild);
        if (refChild == null) {
            this.children.add(newChild);
        } else {
            int at = this.children.indexOf(refChild);
            if (at < 0) {
                throw new DOMException(DOMException.NOT_FOUND_ERR, "refChild not found!");
            }
            this.children.add(at, newChild);
        }
        adopt(newChild);
        return newChild;
    }

    /**
     * Replaces one child with another.
     *
     * @throws IllegalArgumentException if the new one is null
     * @throws DOMException if the old one is not a child of this node
     */
    public Node replaceChild(Node newChild, Node oldChild) {
        if (newChild == null) {
            throw new IllegalArgumentException("newChild == null!");
        }
        int at = this.children.indexOf(oldChild);
        if (at < 0) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, "oldChild not found!");
        }
        detach(newChild);
        this.children.set(at, newChild);
        orphan(oldChild);
        adopt(newChild);
        return oldChild;
    }

    /**
     * Removes a child.
     *
     * @throws IllegalArgumentException if it is null
     * @throws DOMException if it is not a child of this node
     */
    public Node removeChild(Node oldChild) {
        if (oldChild == null) {
            throw new IllegalArgumentException("oldChild == null!");
        }
        int at = this.children.indexOf(oldChild);
        if (at < 0) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, "oldChild not found!");
        }
        this.children.remove(at);
        orphan(oldChild);
        return oldChild;
    }

    /**
     * Appends at the end.
     *
     * @throws IllegalArgumentException if it is null
     */
    public Node appendChild(Node newChild) {
        if (newChild == null) {
            throw new IllegalArgumentException("newChild == null!");
        }
        return insertBefore(newChild, null);
    }

    /** Whether it has any. */
    public boolean hasChildNodes() {
        return !this.children.isEmpty();
    }

    /**
     * A copy.
     *
     * <p>See the class note: it does <b>not</b> copy the attributes, not even in deep mode.
     *
     * @param deep whether to copy the children too
     */
    public Node cloneNode(boolean deep) {
        IIOMetadataNode cloned = new IIOMetadataNode(this.nodeName);
        cloned.setUserObject(getUserObject());
        if (deep) {
            int i = 0;
            while (i < this.children.size()) {
                cloned.appendChild(this.children.get(i).cloneNode(true));
                i = i + 1;
            }
        }
        return cloned;
    }

    /** Does nothing: there are no text nodes to merge. */
    public void normalize() {
    }

    /** Always false: no DOM feature support is declared. */
    public boolean isSupported(String feature, String version) {
        return false;
    }

    /** Null: there are no namespaces. See the class note. */
    public String getNamespaceURI() {
        return null;
    }

    /** Null, for the same reason. */
    public String getPrefix() {
        return null;
    }

    /** Does nothing, for the same reason. */
    public void setPrefix(String prefix) {
    }

    /** The name; without namespaces, local and qualified are the same. */
    public String getLocalName() {
        return this.nodeName;
    }

    /** The name. */
    public String getTagName() {
        return this.nodeName;
    }

    /** The value of that attribute, or the empty string if it is not there. */
    public String getAttribute(String name) {
        Attr attr = getAttributeNode(name);
        if (attr == null) {
            return "";
        }
        return attr.getValue();
    }

    /** Same; the namespace is ignored. */
    public String getAttributeNS(String namespaceURI, String localName) {
        return getAttribute(localName);
    }

    /**
     * Sets an attribute.
     *
     * @throws IllegalArgumentException if the name is null
     */
    public void setAttribute(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("name == null!");
        }
        Attr attr = getAttributeNode(name);
        if (attr != null) {
            attr.setValue(value);
            return;
        }
        this.attributes.add(new IIOAttr(this, name, value));
    }

    /** Same; the namespace is ignored. */
    public void setAttributeNS(String namespaceURI, String qualifiedName, String value) {
        setAttribute(qualifiedName, value);
    }

    /** Removes it; if it was not there, does nothing. */
    public void removeAttribute(String name) {
        removeAttributeByName(name);
    }

    /** Same. */
    public void removeAttributeNS(String namespaceURI, String localName) {
        removeAttribute(localName);
    }

    /** The attribute as a node, or null. */
    public Attr getAttributeNode(String name) {
        Node node = getAttributes().getNamedItem(name);
        return (Attr) node;
    }

    /** Same. */
    public Attr getAttributeNodeNS(String namespaceURI, String localName) {
        return getAttributeNode(localName);
    }

    /**
     * Sets that attribute.
     *
     * @return the one that was there with that name, or null
     * @throws DOMException if the attribute already belongs to another element
     */
    public Attr setAttributeNode(Attr newAttr) throws DOMException {
        Element owner = newAttr.getOwnerElement();
        if (owner != null && owner != this) {
            throw new DOMException(DOMException.INUSE_ATTRIBUTE_ERR,
                                   "Attribute is already in use!");
        }
        Attr old = getAttributeNode(newAttr.getName());
        if (old != null) {
            removeAttributeByName(old.getName());
        }
        if (newAttr instanceof IIOAttr) {
            ((IIOAttr) newAttr).setOwnerElement(this);
        }
        this.attributes.add(newAttr);
        return old;
    }

    /** Same. */
    public Attr setAttributeNodeNS(Attr newAttr) {
        return setAttributeNode(newAttr);
    }

    /**
     * Removes it.
     *
     * @throws DOMException if it is not an attribute of this element
     */
    public Attr removeAttributeNode(Attr oldAttr) {
        int at = this.attributes.indexOf(oldAttr);
        if (at < 0) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, "Attribute not found!");
        }
        this.attributes.remove(at);
        if (oldAttr instanceof IIOAttr) {
            ((IIOAttr) oldAttr).setOwnerElement(null);
        }
        return oldAttr;
    }

    /**
     * The descendants with that name, in traversal order.
     *
     * <p>It is a snapshot, not a live view; see {@link IIONodeList}.
     */
    public NodeList getElementsByTagName(String name) {
        List<Node> found = new ArrayList<Node>();
        collectByName(this, name, found);
        return new IIONodeList(found);
    }

    /** Same. */
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        return getElementsByTagName(localName);
    }

    /** Whether it has any. */
    public boolean hasAttributes() {
        return !this.attributes.isEmpty();
    }

    /** Whether it has that one. */
    public boolean hasAttribute(String name) {
        return getAttributeNode(name) != null;
    }

    /** Same. */
    public boolean hasAttributeNS(String namespaceURI, String localName) {
        return hasAttribute(localName);
    }

    /** How many children. It is the {@link NodeList} part. */
    public int getLength() {
        return this.children.size();
    }

    /** Child number {@code index}, or null if it does not exist. Never throws. */
    public Node item(int index) {
        if (index < 0 || index >= this.children.size()) {
            return null;
        }
        return this.children.get(index);
    }

    /** The data that is not text, or null. See the class note. */
    public Object getUserObject() {
        return this.userObject;
    }

    /** Changes it. */
    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }

    /** Not supported; see the class note. */
    public void setIdAttribute(String name, boolean isId) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public void setIdAttributeNode(Attr idAttr, boolean isId) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public TypeInfo getSchemaTypeInfo() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public Object setUserData(String key, Object data, UserDataHandler handler) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public Object getUserData(String key) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public Object getFeature(String feature, String version) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public boolean isSameNode(Node node) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public boolean isEqualNode(Node node) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public String lookupNamespaceURI(String prefix) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public boolean isDefaultNamespace(String namespaceURI) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public String lookupPrefix(String namespaceURI) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public String getTextContent() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public void setTextContent(String textContent) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public short compareDocumentPosition(Node other) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public String getBaseURI() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** The sibling at that distance, or null. */
    private Node siblingAt(int delta) {
        if (this.parent == null) {
            return null;
        }
        NodeList siblings = this.parent.getChildNodes();
        int i = 0;
        while (i < siblings.getLength()) {
            if (siblings.item(i) == this) {
                return siblings.item(i + delta);
            }
            i = i + 1;
        }
        return null;
    }

    /** Removes it from wherever it was before putting it here. */
    private void detach(Node node) {
        Node oldParent = node.getParentNode();
        if (oldParent != null && oldParent != this) {
            oldParent.removeChild(node);
        } else if (oldParent == this) {
            this.children.remove(node);
        }
    }

    /** Records that this is its parent. */
    private void adopt(Node node) {
        if (node instanceof IIOMetadataNode) {
            ((IIOMetadataNode) node).parent = this;
        }
    }

    /** Clears its parent. */
    private void orphan(Node node) {
        if (node instanceof IIOMetadataNode) {
            ((IIOMetadataNode) node).parent = null;
        }
    }

    /** Removes the attribute with that name, if it is there. */
    private void removeAttributeByName(String name) {
        int i = 0;
        while (i < this.attributes.size()) {
            if (name.equals(this.attributes.get(i).getNodeName())) {
                Node removed = this.attributes.remove(i);
                if (removed instanceof IIOAttr) {
                    ((IIOAttr) removed).setOwnerElement(null);
                }
                return;
            }
            i = i + 1;
        }
    }

    /** Collects the descendants with that name, in traversal order. */
    private static void collectByName(IIOMetadataNode node, String name, List<Node> into) {
        if (name.equals(node.getNodeName())) {
            into.add(node);
        }
        int i = 0;
        while (i < node.children.size()) {
            Node child = node.children.get(i);
            if (child instanceof IIOMetadataNode) {
                collectByName((IIOMetadataNode) child, name, into);
            }
            i = i + 1;
        }
    }
}
