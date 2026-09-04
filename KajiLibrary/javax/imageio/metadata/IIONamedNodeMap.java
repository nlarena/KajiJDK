package javax.imageio.metadata;

import java.util.List;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

/**
 * Los atributos de un {@link IIOMetadataNode}, vistos como mapa del DOM.
 *
 * <p>De acceso de paquete: no es API. La devuelve {@code Node.getAttributes}.
 *
 * <p>Es de <b>solo lectura</b>: {@code setNamedItem} y {@code removeNamedItem} lanzan
 * {@link DOMException} con {@code NO_MODIFICATION_ALLOWED_ERR}. Para cambiar atributos estan los
 * metodos del propio elemento.
 *
 * <p>Igual que {@link IIONodeList}, es una foto tomada al pedirla.
 */
class IIONamedNodeMap implements NamedNodeMap {

    /** Los atributos. */
    private final List<Node> nodes;

    IIONamedNodeMap(List<Node> nodes) {
        this.nodes = nodes;
    }

    public int getLength() {
        return this.nodes.size();
    }

    /** El atributo con ese nombre, o null. */
    public Node getNamedItem(String name) {
        int i = 0;
        while (i < this.nodes.size()) {
            if (name.equals(this.nodes.get(i).getNodeName())) {
                return this.nodes.get(i);
            }
            i = i + 1;
        }
        return null;
    }

    /** El numero {@code index}, o null. */
    public Node item(int index) {
        if (index < 0 || index >= this.nodes.size()) {
            return null;
        }
        return this.nodes.get(index);
    }

    /** No: es de solo lectura. Ver la nota de la clase. */
    public Node removeNamedItem(String name) {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                               "This NamedNodeMap is read-only!");
    }

    /** No: es de solo lectura. */
    public Node setNamedItem(Node arg) {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                               "This NamedNodeMap is read-only!");
    }

    /** Los espacios de nombres no se soportan; ver {@link IIOMetadataNode}. */
    public Node getNamedItemNS(String namespaceURI, String localName) {
        return getNamedItem(localName);
    }

    /** No: es de solo lectura. */
    public Node setNamedItemNS(Node arg) {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                               "This NamedNodeMap is read-only!");
    }

    /** No: es de solo lectura. */
    public Node removeNamedItemNS(String namespaceURI, String localName) {
        throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                               "This NamedNodeMap is read-only!");
    }
}
