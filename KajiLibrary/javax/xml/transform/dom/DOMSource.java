package javax.xml.transform.dom;

import javax.xml.transform.Source;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.transform.dom.DOMSource -- un arbol DOM como fuente de una transformacion.
 *
 * <p>Lleva un nodo, no necesariamente un documento. Esa es la parte que importa: se puede transformar
 * <b>cualquier subarbol</b> pasando el elemento que lo encabeza, sin copiarlo ni sacarlo del
 * documento donde vive.
 *
 * <p>El identificador de sistema va aparte del nodo porque un arbol en memoria no sabe de donde
 * salio. Se usa para resolver referencias relativas --un {@code document()} dentro de la hoja de
 * estilo, por ejemplo-- y por eso conviene ponerlo aunque el nodo ya este armado.
 */
public class DOMSource implements Source {

    /** Con esto se le pregunta a un {@code TransformerFactory} si acepta esta fuente. */
    public static final String FEATURE = "http://javax.xml.transform.dom.DOMSource/feature";

    private Node node;

    private String systemId;

    /** Vacia, para llenarla con {@link #setNode}. */
    public DOMSource() {
    }

    /**
     * Con un nodo.
     *
     * @param n cualquier nodo, no solo un documento; ver la nota de la clase
     */
    public DOMSource(Node n) {
        setNode(n);
    }

    /**
     * Con un nodo y de donde salio.
     *
     * @param systemId contra el que se resuelve lo relativo
     */
    public DOMSource(Node node, String systemId) {
        setNode(node);
        setSystemId(systemId);
    }

    /** El nodo a transformar. */
    public void setNode(Node node) {
        this.node = node;
    }

    /** Ver {@link #setNode}. */
    public Node getNode() {
        return this.node;
    }

    /** De donde salio el arbol. Ver la nota de la clase. */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /** Ver {@link #setSystemId}. */
    public String getSystemId() {
        return this.systemId;
    }

    /**
     * Si esta fuente no dice nada.
     *
     * <p>Mira el nodo <b>y</b> el identificador, y hace falta explicarlo porque el resultado
     * sorprende: una fuente con identificador y sin nodo cuenta como no vacia, aunque esta clase no
     * sepa ir a buscar nada a ese identificador.
     *
     * <p>Tiene sentido igual. La pregunta no es "tengo un arbol" sino "me dieron algo": una fuente
     * con identificador es una que alguien lleno, y quien la recibe puede resolverla por su cuenta.
     * Contestar que esta vacia haria que se descartara en silencio lo unico que se le puso.
     */
    public boolean isEmpty() {
        return getNode() == null && getSystemId() == null;
    }
}
