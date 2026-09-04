package javax.imageio.metadata;

import javax.imageio.IIOException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.imageio.metadata.IIOInvalidTreeException -- ese arbol de metadatos no sirve.
 *
 * <p>La lanzan {@code IIOMetadata.setFromTree} y {@code mergeTree} cuando el arbol que se les da no
 * respeta el formato declarado.
 *
 * <p>Lo que la hace util es {@link #getOffendingNode}: dice <b>cual</b> nodo esta mal. Un arbol de
 * metadatos tiene decenas de nodos y un mensaje del estilo "atributo invalido" sin decir donde obliga
 * a buscarlo a mano.
 *
 * <p>Puede devolver null si el problema es del arbol entero --la raiz no es la que el formato pide--
 * y no de un nodo en particular.
 */
public class IIOInvalidTreeException extends IIOException {

    private static final long serialVersionUID = -1314083172544132777L;

    /** Cual nodo esta mal, o null. */
    protected Node offendingNode = null;

    /**
     * @param message que esta mal
     * @param offendingNode cual nodo, o null
     */
    public IIOInvalidTreeException(String message, Node offendingNode) {
        super(message);
        this.offendingNode = offendingNode;
    }

    /**
     * Idem, envolviendo la original.
     *
     * @param cause lo que fallo mientras se recorria el arbol
     */
    public IIOInvalidTreeException(String message, Throwable cause, Node offendingNode) {
        super(message, cause);
        this.offendingNode = offendingNode;
    }

    /** Cual nodo esta mal, o null. Ver la nota de la clase. */
    public Node getOffendingNode() {
        return this.offendingNode;
    }
}
