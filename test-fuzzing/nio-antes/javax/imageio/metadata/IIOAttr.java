package javax.imageio.metadata;

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
 * An attribute of an {@link IIOMetadataNode}.
 *
 * <p>Package-private: it is not API. It exists because {@code Element.getAttributeNode} has to
 * return an {@link Attr}, and a pair of strings in a map is not one.
 *
 * <p>It is deliberately minimal. Everything from DOM level 3 --schema types, user data, position
 * comparison-- throws {@link DOMException} with {@code NOT_SUPPORTED_ERR}, as in
 * {@link IIOMetadataNode}: image metadata is a tree of names and values, not a full XML
 * document.
 */
class IIOAttr implements Attr {

    /** What it is called. */
    private final String name;

    /** What it is worth. */
    private String value;

    /** Which element it belongs to. */
    private Element owner;

    IIOAttr(Element owner, String name, String value) {
        this.owner = owner;
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public String getNodeName() {
        return this.name;
    }

    public short getNodeType() {
        return ATTRIBUTE_NODE;
    }

    /** Always true: an attribute without a value is not stored. */
    public boolean getSpecified() {
        return true;
    }

    public String getValue() {
        return this.value;
    }

    public String getNodeValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setNodeValue(String value) {
        this.value = value;
    }

    public Element getOwnerElement() {
        return this.owner;
    }

    /** Package-private: {@link IIOMetadataNode} uses it when moving attributes. */
    void setOwnerElement(Element owner) {
        this.owner = owner;
    }

    /** An attribute has no parent in the DOM sense; its owner is the element. */
    public Node getParentNode() {
        return null;
    }

    public NodeList getChildNodes() {
        return new IIONodeList(new java.util.ArrayList<Node>());
    }

    public Node getFirstChild() {
        return null;
    }

    public Node getLastChild() {
        return null;
    }

    public Node getPreviousSibling() {
        return null;
    }

    public Node getNextSibling() {
        return null;
    }

    public NamedNodeMap getAttributes() {
        return null;
    }

    public Document getOwnerDocument() {
        return null;
    }

    public Node insertBefore(Node newChild, Node refChild) {
        return null;
    }

    public Node replaceChild(Node newChild, Node oldChild) {
        return null;
    }

    public Node removeChild(Node oldChild) {
        return null;
    }

    public Node appendChild(Node newChild) {
        return null;
    }

    public boolean hasChildNodes() {
        return false;
    }

    public Node cloneNode(boolean deep) {
        return new IIOAttr(this.owner, this.name, this.value);
    }

    public void normalize() {
    }

    public boolean isSupported(String feature, String version) {
        return false;
    }

    public String getNamespaceURI() {
        return null;
    }

    public String getPrefix() {
        return null;
    }

    public void setPrefix(String prefix) {
    }

    public String getLocalName() {
        return this.name;
    }

    public boolean hasAttributes() {
        return false;
    }

    /** Not supported; see the class note. */
    public TypeInfo getSchemaTypeInfo() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** Not supported. */
    public boolean isId() {
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
}
