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
 * KajiLibrary's javax.imageio.metadata.IIOMetadataNode -- un nodo de arbol de metadatos de imagen.
 *
 * <p>Implementa {@link org.w3c.dom.Element} para que los metadatos se puedan recorrer con las
 * herramientas de DOM de siempre, sin obligar a nadie a aprender una API nueva.
 *
 * <p>Pero <b>no es un DOM completo</b>, y conviene saber en que se queda corto:
 *
 * <ul>
 *   <li>no hay espacios de nombres. Los metodos {@code xxxNS} existen y se comportan como los que no
 *       lo son; {@link #getNamespaceURI} devuelve null;
 *   <li>no hay documento duenio: {@link #getOwnerDocument} devuelve null. Un arbol de metadatos flota
 *       solo;
 *   <li>todo el nivel 3 --tipos de esquema, datos de usuario, comparacion de posicion, contenido de
 *       texto-- lanza {@link DOMException} con {@code NOT_SUPPORTED_ERR}.
 * </ul>
 *
 * <h2>El objeto de usuario</h2>
 *
 * <p>{@link #getUserObject} es lo que hace util a esta clase frente a un DOM de verdad. Un arbol de
 * metadatos a veces necesita llevar un dato que no es texto --una tabla de cuantizacion, un perfil de
 * color, un arreglo de bytes-- y meterlo como cadena seria absurdo.
 *
 * <p>Cuando hay objeto de usuario, los atributos y los hijos suelen sobrar: el nodo <b>es</b> ese
 * objeto.
 *
 * <h2>El nodo es su propia lista de hijos</h2>
 *
 * <p>Implementa tambien {@link NodeList}, y {@link #getChildNodes} se devuelve a si mismo. Es un
 * atajo de implementacion del JDK que se nota: {@code n.getChildNodes() == n} es cierto.
 *
 * <h2>{@link #cloneNode} no copia los atributos</h2>
 *
 * <p>Ni siquiera en modo profundo. Copia el nombre, el objeto de usuario y --si es profundo-- los
 * hijos, y nada mas. Se comprobo contra el JDK 25 y es asi; parece un descuido de 1999 que ya no se
 * puede arreglar sin romper a quien dependa de ello.
 */
public class IIOMetadataNode implements Element, NodeList {

    /** Como se llama. */
    private String nodeName;

    /** El valor de texto, casi siempre null. */
    private String nodeValue;

    /** Los atributos. */
    private final List<Node> attributes = new ArrayList<Node>();

    /** Los hijos, en orden. */
    private final List<Node> children = new ArrayList<Node>();

    /** De quien es hijo, o null. */
    private Node parent;

    /** El dato que no es texto. Ver la nota de la clase. */
    private Object userObject;

    /** Sin nombre; el nombre queda en null. */
    public IIOMetadataNode() {
        this.nodeName = null;
    }

    /** @param nodeName como se llama */
    public IIOMetadataNode(String nodeName) {
        this.nodeName = nodeName;
    }

    /** Como se llama. */
    public String getNodeName() {
        return this.nodeName;
    }

    /** El valor de texto, o null. */
    public String getNodeValue() {
        return this.nodeValue;
    }

    /** Lo cambia. */
    public void setNodeValue(String nodeValue) {
        this.nodeValue = nodeValue;
    }

    /** Siempre {@link Node#ELEMENT_NODE}. */
    public short getNodeType() {
        return ELEMENT_NODE;
    }

    /** De quien es hijo, o null. */
    public Node getParentNode() {
        return this.parent;
    }

    /** Se devuelve a si mismo. Ver la nota de la clase. */
    public NodeList getChildNodes() {
        return this;
    }

    /** El primer hijo, o null. */
    public Node getFirstChild() {
        if (this.children.isEmpty()) {
            return null;
        }
        return this.children.get(0);
    }

    /** El ultimo, o null. */
    public Node getLastChild() {
        if (this.children.isEmpty()) {
            return null;
        }
        return this.children.get(this.children.size() - 1);
    }

    /** El hermano anterior, o null. */
    public Node getPreviousSibling() {
        return siblingAt(-1);
    }

    /** El siguiente, o null. */
    public Node getNextSibling() {
        return siblingAt(1);
    }

    /** Los atributos, de solo lectura. Ver {@link IIONamedNodeMap}. */
    public NamedNodeMap getAttributes() {
        return new IIONamedNodeMap(new ArrayList<Node>(this.attributes));
    }

    /** Null: un arbol de metadatos no tiene documento. Ver la nota de la clase. */
    public Document getOwnerDocument() {
        return null;
    }

    /**
     * Inserta antes de ese hijo; con {@code refChild} null, agrega al final.
     *
     * @throws IllegalArgumentException si el nuevo hijo es null
     * @throws DOMException si el de referencia no es hijo de este nodo
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
     * Reemplaza un hijo por otro.
     *
     * @throws IllegalArgumentException si el nuevo es null
     * @throws DOMException si el viejo no es hijo de este nodo
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
     * Saca un hijo.
     *
     * @throws IllegalArgumentException si es null
     * @throws DOMException si no es hijo de este nodo
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
     * Agrega al final.
     *
     * @throws IllegalArgumentException si es null
     */
    public Node appendChild(Node newChild) {
        if (newChild == null) {
            throw new IllegalArgumentException("newChild == null!");
        }
        return insertBefore(newChild, null);
    }

    /** Si tiene alguno. */
    public boolean hasChildNodes() {
        return !this.children.isEmpty();
    }

    /**
     * Una copia.
     *
     * <p>Ver la nota de la clase: <b>no</b> copia los atributos, ni siquiera en modo profundo.
     *
     * @param deep si copiar tambien los hijos
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

    /** No hace nada: no hay nodos de texto que juntar. */
    public void normalize() {
    }

    /** Siempre false: no se declara soporte de ninguna caracteristica del DOM. */
    public boolean isSupported(String feature, String version) {
        return false;
    }

    /** Null: no hay espacios de nombres. Ver la nota de la clase. */
    public String getNamespaceURI() {
        return null;
    }

    /** Null, por lo mismo. */
    public String getPrefix() {
        return null;
    }

    /** No hace nada, por lo mismo. */
    public void setPrefix(String prefix) {
    }

    /** El nombre; sin espacios de nombres, local y completo son el mismo. */
    public String getLocalName() {
        return this.nodeName;
    }

    /** El nombre. */
    public String getTagName() {
        return this.nodeName;
    }

    /** El valor de ese atributo, o la cadena vacia si no esta. */
    public String getAttribute(String name) {
        Attr attr = getAttributeNode(name);
        if (attr == null) {
            return "";
        }
        return attr.getValue();
    }

    /** Idem; el espacio de nombres se ignora. */
    public String getAttributeNS(String namespaceURI, String localName) {
        return getAttribute(localName);
    }

    /**
     * Fija un atributo.
     *
     * @throws IllegalArgumentException si el nombre es null
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

    /** Idem; el espacio de nombres se ignora. */
    public void setAttributeNS(String namespaceURI, String qualifiedName, String value) {
        setAttribute(qualifiedName, value);
    }

    /** Lo saca; si no estaba, no hace nada. */
    public void removeAttribute(String name) {
        removeAttributeByName(name);
    }

    /** Idem. */
    public void removeAttributeNS(String namespaceURI, String localName) {
        removeAttribute(localName);
    }

    /** El atributo como nodo, o null. */
    public Attr getAttributeNode(String name) {
        Node node = getAttributes().getNamedItem(name);
        return (Attr) node;
    }

    /** Idem. */
    public Attr getAttributeNodeNS(String namespaceURI, String localName) {
        return getAttributeNode(localName);
    }

    /**
     * Pone ese atributo.
     *
     * @return el que estaba con ese nombre, o null
     * @throws DOMException si el atributo ya pertenece a otro elemento
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

    /** Idem. */
    public Attr setAttributeNodeNS(Attr newAttr) {
        return setAttributeNode(newAttr);
    }

    /**
     * Lo saca.
     *
     * @throws DOMException si no es un atributo de este elemento
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
     * Los descendientes con ese nombre, en orden de recorrido.
     *
     * <p>Es una foto, no una vista viva; ver {@link IIONodeList}.
     */
    public NodeList getElementsByTagName(String name) {
        List<Node> found = new ArrayList<Node>();
        collectByName(this, name, found);
        return new IIONodeList(found);
    }

    /** Idem. */
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        return getElementsByTagName(localName);
    }

    /** Si tiene alguno. */
    public boolean hasAttributes() {
        return !this.attributes.isEmpty();
    }

    /** Si tiene ese. */
    public boolean hasAttribute(String name) {
        return getAttributeNode(name) != null;
    }

    /** Idem. */
    public boolean hasAttributeNS(String namespaceURI, String localName) {
        return hasAttribute(localName);
    }

    /** Cuantos hijos. Es la parte de {@link NodeList}. */
    public int getLength() {
        return this.children.size();
    }

    /** El hijo numero {@code index}, o null si no existe. Nunca lanza. */
    public Node item(int index) {
        if (index < 0 || index >= this.children.size()) {
            return null;
        }
        return this.children.get(index);
    }

    /** El dato que no es texto, o null. Ver la nota de la clase. */
    public Object getUserObject() {
        return this.userObject;
    }

    /** Lo cambia. */
    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }

    /** No soportado; ver la nota de la clase. */
    public void setIdAttribute(String name, boolean isId) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public void setIdAttributeNode(Attr idAttr, boolean isId) {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
    }

    /** No soportado. */
    public TypeInfo getSchemaTypeInfo() {
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

    /** El hermano que esta a esa distancia, o null. */
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

    /** Lo saca de donde estuviera antes de meterlo aca. */
    private void detach(Node node) {
        Node oldParent = node.getParentNode();
        if (oldParent != null && oldParent != this) {
            oldParent.removeChild(node);
        } else if (oldParent == this) {
            this.children.remove(node);
        }
    }

    /** Le anota que este es su padre. */
    private void adopt(Node node) {
        if (node instanceof IIOMetadataNode) {
            ((IIOMetadataNode) node).parent = this;
        }
    }

    /** Le borra el padre. */
    private void orphan(Node node) {
        if (node instanceof IIOMetadataNode) {
            ((IIOMetadataNode) node).parent = null;
        }
    }

    /** Saca el atributo con ese nombre, si esta. */
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

    /** Junta los descendientes con ese nombre, en orden de recorrido. */
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
