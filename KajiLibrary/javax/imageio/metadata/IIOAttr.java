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
 * Un atributo de un {@link IIOMetadataNode}.
 *
 * <p>De acceso de paquete: no es API. Existe porque {@code Element.getAttributeNode} tiene que
 * devolver un {@link Attr}, y un par de cadenas en un mapa no lo es.
 *
 * <p>Es deliberadamente minimo. Todo lo del nivel 3 del DOM --tipos de esquema, datos de usuario,
 * comparacion de posicion-- lanza {@link DOMException} con {@code NOT_SUPPORTED_ERR}, igual que en
 * {@link IIOMetadataNode}: los metadatos de imagen son un arbol de nombres y valores, no un documento
 * XML completo.
 */
class IIOAttr implements Attr {

    /** Como se llama. */
    private final String name;

    /** Que vale. */
    private String value;

    /** De que elemento es. */
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

    /** Siempre true: un atributo sin valor no se guarda. */
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

    /** De acceso de paquete: lo usa {@link IIOMetadataNode} al mover atributos. */
    void setOwnerElement(Element owner) {
        this.owner = owner;
    }

    /** Un atributo no tiene padre en el sentido del DOM; su duena es el elemento. */
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

    /** No soportado; ver la nota de la clase. */
    public TypeInfo getSchemaTypeInfo() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public boolean isId() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public Object setUserData(String key, Object data, UserDataHandler handler) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public Object getUserData(String key) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public Object getFeature(String feature, String version) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public boolean isSameNode(Node node) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public boolean isEqualNode(Node node) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public String lookupNamespaceURI(String prefix) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public boolean isDefaultNamespace(String namespaceURI) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public String lookupPrefix(String namespaceURI) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public String getTextContent() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public void setTextContent(String textContent) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public short compareDocumentPosition(Node other) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public String getBaseURI() {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }
}
