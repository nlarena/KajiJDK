package javax.xml.transform.dom;

import javax.xml.transform.Result;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.transform.dom.DOMResult -- un arbol DOM como destino de una transformacion.
 *
 * <p>Se puede usar de dos formas. Sin nodo, el transformador crea un documento nuevo y lo deja en
 * {@link #getNode}. Con nodo, la salida se <b>agrega</b> a ese nodo, que es lo que permite armar un
 * documento a pedazos con varias transformaciones.
 *
 * <h2>Donde se inserta</h2>
 *
 * <p>{@link #setNextSibling} decide el lugar exacto: sin el, la salida va al final de los hijos; con
 * el, justo antes de ese hermano. Es la unica forma de insertar en el medio, porque el
 * transformador no sabe nada del documento salvo lo que dice este objeto.
 *
 * <p>De ahi sale la validacion que sorprende: el hermano tiene que ser <b>hijo del nodo</b>, y si no
 * lo es, se rechaza. Tiene que ser asi -- un hermano que vive en otra parte del arbol describiria un
 * lugar que no existe dentro del nodo destino--, y la validacion pasa al construir en vez de al
 * transformar, que es cuando ya seria tarde para arreglarlo.
 *
 * <p>Las dos vias contestan con excepciones distintas y no es un descuido: en el constructor es
 * {@link IllegalArgumentException} porque son argumentos incoherentes entre si, y en
 * {@link #setNextSibling} es {@link IllegalStateException} porque el argumento se contradice con el
 * nodo que el objeto <b>ya tenia</b>.
 */
public class DOMResult implements Result {

    /** Con esto se le pregunta a un {@code TransformerFactory} si acepta este destino. */
    public static final String FEATURE = "http://javax.xml.transform.dom.DOMResult/feature";

    private Node node;

    private Node nextSibling;

    private String systemId;

    /** Vacio: el transformador crea el documento. */
    public DOMResult() {
        setNode(null);
        setNextSibling(null);
        setSystemId(null);
    }

    /** La salida se agrega al final de los hijos de ese nodo. */
    public DOMResult(Node node) {
        setNode(node);
        setNextSibling(null);
        setSystemId(null);
    }

    /** Idem, diciendo de donde sale el resultado. */
    public DOMResult(Node node, String systemId) {
        setNode(node);
        setNextSibling(null);
        setSystemId(systemId);
    }

    /**
     * La salida se inserta antes de ese hermano.
     *
     * @throws IllegalArgumentException si el hermano no es hijo del nodo; ver la nota de la clase
     */
    public DOMResult(Node node, Node nextSibling) {
        if (nextSibling != null) {
            if (node == null) {
                throw new IllegalArgumentException(
                    "Cannot create a DOMResult when the nextSibling is contained by the "
                        + "\"null\" node.");
            }
            if (nextSibling.getParentNode() != node) {
                throw new IllegalArgumentException(
                    "Cannot create a DOMResult when the nextSibling is not contained by the node.");
            }
        }
        setNode(node);
        this.nextSibling = nextSibling;
        setSystemId(null);
    }

    /**
     * Las dos cosas.
     *
     * @throws IllegalArgumentException si el hermano no es hijo del nodo
     */
    public DOMResult(Node node, Node nextSibling, String systemId) {
        this(node, nextSibling);
        setSystemId(systemId);
    }

    /** El nodo al que se le agrega la salida; null para que el transformador cree uno. */
    public void setNode(Node node) {
        this.node = node;
    }

    /** Ver {@link #setNode}. */
    public Node getNode() {
        return this.node;
    }

    /**
     * Antes de que hermano se inserta la salida; null para agregar al final.
     *
     * @throws IllegalStateException si no es hijo del nodo que ya tiene este objeto
     */
    public void setNextSibling(Node nextSibling) {
        if (nextSibling != null) {
            if (this.node == null) {
                throw new IllegalStateException(
                    "Cannot create a DOMResult when the nextSibling is contained by the "
                        + "\"null\" node.");
            }
            if (nextSibling.getParentNode() != this.node) {
                throw new IllegalStateException(
                    "Cannot create a DOMResult when the nextSibling is not contained by the node.");
            }
        }
        this.nextSibling = nextSibling;
    }

    /** Ver {@link #setNextSibling}. */
    public Node getNextSibling() {
        return this.nextSibling;
    }

    /** De donde sale el resultado; informativo, no se escribe nada ahi. */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /** Ver {@link #setSystemId}. */
    public String getSystemId() {
        return this.systemId;
    }
}
