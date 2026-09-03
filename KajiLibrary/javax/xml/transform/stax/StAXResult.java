package javax.xml.transform.stax;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;

/**
 * KajiLibrary's javax.xml.transform.stax.StAXResult -- un escritor StAX como destino de una
 * transformacion.
 *
 * <p>El espejo de {@link StAXSource}: lleva <b>uno</b> de los dos escritores y el getter del otro
 * devuelve null.
 *
 * <p>Sirve para encadenar sin materializar nada en el medio: la salida de una transformacion se
 * escribe directo por el mismo escritor con el que el programa ya venia escribiendo, en vez de
 * pasar por un arbol o por texto.
 *
 * <p>{@link #setSystemId} lanza, por la misma razon que en {@link StAXSource}: el destino lo decide
 * el escritor, no quien arma el {@code Result}.
 */
public class StAXResult implements Result {

    /** Con esto se le pregunta a un {@code TransformerFactory} si acepta este destino. */
    public static final String FEATURE = "http://javax.xml.transform.stax.StAXResult/feature";

    /** Uno de los dos es null. */
    private XMLEventWriter eventWriter;

    private XMLStreamWriter streamWriter;

    /**
     * Con un escritor de eventos.
     *
     * @throws IllegalArgumentException si es null
     */
    public StAXResult(XMLEventWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException(
                "StAXResult(XMLEventWriter) with XMLEventWriter == null");
        }
        this.eventWriter = writer;
    }

    /**
     * Con un escritor de flujo.
     *
     * @throws IllegalArgumentException si es null
     */
    public StAXResult(XMLStreamWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException(
                "StAXResult(XMLStreamWriter) with XMLStreamWriter == null");
        }
        this.streamWriter = writer;
    }

    /** El escritor de eventos, o null si se construyo con el de flujo. */
    public XMLEventWriter getXMLEventWriter() {
        return this.eventWriter;
    }

    /** El escritor de flujo, o null si se construyo con el de eventos. */
    public XMLStreamWriter getXMLStreamWriter() {
        return this.streamWriter;
    }

    /**
     * No se puede cambiar.
     *
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void setSystemId(String systemId) {
        throw new UnsupportedOperationException(
            "StAXResult#setSystemId(systemId) cannot set the system identifier for a StAXResult");
    }

    /** Siempre null: el destino lo sabe el escritor. */
    public String getSystemId() {
        return null;
    }
}
