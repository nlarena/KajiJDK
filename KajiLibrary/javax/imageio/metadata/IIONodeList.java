package javax.imageio.metadata;

import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Una lista de nodos ya materializada.
 *
 * <p>De acceso de paquete: no es API. La devuelven {@code getElementsByTagName} y companeros.
 *
 * <p>Es una foto, no una vista viva: a diferencia de lo que el DOM promete para
 * {@code getElementsByTagName}, agregar un nodo despues no la cambia. Es lo que hace el JDK, y para
 * metadatos de imagen --que se recorren y no se editan mientras se recorren-- alcanza.
 */
class IIONodeList implements NodeList {

    /** Los nodos. */
    private final List<Node> nodes;

    IIONodeList(List<Node> nodes) {
        this.nodes = nodes;
    }

    public int getLength() {
        return this.nodes.size();
    }

    /** El nodo numero {@code index}, o null si no existe. Nunca lanza. */
    public Node item(int index) {
        if (index < 0 || index >= this.nodes.size()) {
            return null;
        }
        return this.nodes.get(index);
    }
}
