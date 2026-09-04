package javax.xml.crypto.dom;

import javax.xml.crypto.XMLStructure;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.crypto.dom.DOMStructure -- un nodo DOM visto como estructura de firma XML.
 *
 * <p>El adaptador entre los dos mundos. Las APIs de firma hablan {@link XMLStructure}; el contenido
 * concreto de una firma --el {@code KeyInfo} de un formato propio, un {@code Object} con datos
 * arbitrarios-- llega como DOM. Esta clase es el puente, y es de una linea.
 *
 * <p>Es inmutable y no copia el nodo: guarda la referencia. Modificar el arbol despues de envolverlo
 * cambia lo que se firma.
 *
 * <p>{@link #isFeatureSupported} devuelve false para todo, incluido el mecanismo {@code "DOM"}. Es lo
 * que hace el JDK: la clase no soporta ninguna caracteristica declarable.
 */
public class DOMStructure implements XMLStructure {

    /** El nodo envuelto. */
    private final Node node;

    /**
     * @param node el nodo; no se copia
     * @throws NullPointerException si es null
     */
    public DOMStructure(Node node) {
        if (node == null) {
            throw new NullPointerException("node cannot be null");
        }
        this.node = node;
    }

    /** El nodo envuelto. */
    public Node getNode() {
        return this.node;
    }

    /**
     * Siempre false. Ver la nota de la clase.
     *
     * @throws NullPointerException si el nombre es null
     */
    public boolean isFeatureSupported(String feature) {
        if (feature == null) {
            throw new NullPointerException();
        }
        return false;
    }
}
