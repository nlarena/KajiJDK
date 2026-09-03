package javax.xml.transform.stax;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;

/**
 * KajiLibrary's javax.xml.transform.stax.StAXSource -- un lector StAX como fuente de una
 * transformacion.
 *
 * <p>Lleva <b>uno</b> de los dos lectores de StAX, el de flujo o el de eventos, y nunca los dos: el
 * getter del que no se paso devuelve null. Es asi porque los dos son la misma lectura vista de dos
 * formas, y convertir uno en el otro consumiria el que se recibio.
 *
 * <h2>El lector tiene que estar al principio de algo</h2>
 *
 * <p>El constructor exige que el lector este parado en {@code START_DOCUMENT} o
 * {@code START_ELEMENT}, y si no, lanza. La validacion vale la pena: un lector a medio consumir
 * produciria una transformacion de un fragmento arbitrario, y el error recien aparece mucho despues,
 * cuando la salida sale cortada.
 *
 * <p>Que acepte {@code START_ELEMENT} y no solo {@code START_DOCUMENT} es a proposito: deja
 * transformar <b>un subarbol</b> de un documento grande sin sacarlo aparte.
 *
 * <h2>El identificador de sistema es de solo lectura</h2>
 *
 * <p>{@link #setSystemId} lanza {@link UnsupportedOperationException}, que es raro para un setter y
 * es correcto: el identificador sale de la posicion del lector, que es quien sabe de donde vino lo
 * que esta leyendo. Dejarlo cambiar permitiria mentir sobre el origen, y las referencias relativas se
 * resolverian contra una base falsa.
 */
public class StAXSource implements Source {

    /** Con esto se le pregunta a un {@code TransformerFactory} si acepta esta fuente. */
    public static final String FEATURE = "http://javax.xml.transform.stax.StAXSource/feature";

    /** Uno de los dos es null; ver la nota de la clase. */
    private XMLStreamReader streamReader;

    private XMLEventReader eventReader;

    /** El del lector, cacheado al construir. */
    private String systemId;

    /**
     * Con un lector de eventos.
     *
     * @throws IllegalArgumentException si es null
     * @throws XMLStreamException si no esta al principio de un documento o de un elemento
     */
    public StAXSource(XMLEventReader reader) throws XMLStreamException {
        if (reader == null) {
            throw new IllegalArgumentException(
                "StAXSource(XMLEventReader) with XMLEventReader == null");
        }
        // `peek` y no `nextEvent`: mirar no puede consumir, porque quien lo reciba tiene que poder
        // leer desde el principio.
        XMLEvent event = reader.peek();
        int type = (event == null) ? -1 : event.getEventType();
        if (type != XMLStreamConstants.START_DOCUMENT
                && type != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException(
                "StAXSource(XMLEventReader) with XMLEventReader not in "
                    + "XMLStreamConstants.START_DOCUMENT or XMLStreamConstants.START_ELEMENT state");
        }
        this.eventReader = reader;
        this.systemId = (event.getLocation() == null) ? null : event.getLocation().getSystemId();
    }

    /**
     * Con un lector de flujo.
     *
     * @throws IllegalArgumentException si es null
     * @throws IllegalStateException si no esta al principio de un documento o de un elemento
     */
    public StAXSource(XMLStreamReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException(
                "StAXSource(XMLStreamReader) with XMLStreamReader == null");
        }
        int type = reader.getEventType();
        if (type != XMLStreamConstants.START_DOCUMENT
                && type != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException(
                "StAXSource(XMLStreamReader) with XMLStreamReadernot in "
                    + "XMLStreamConstants.START_DOCUMENT or XMLStreamConstants.START_ELEMENT state");
        }
        this.streamReader = reader;
        this.systemId = (reader.getLocation() == null) ? null : reader.getLocation().getSystemId();
    }

    /** El lector de eventos, o null si se construyo con el de flujo. */
    public XMLEventReader getXMLEventReader() {
        return this.eventReader;
    }

    /** El lector de flujo, o null si se construyo con el de eventos. */
    public XMLStreamReader getXMLStreamReader() {
        return this.streamReader;
    }

    /**
     * No se puede cambiar.
     *
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void setSystemId(String systemId) {
        throw new UnsupportedOperationException(
            "StAXSource#setSystemId(systemId) cannot set the system identifier for a StAXSource");
    }

    /** De donde dice el lector que viene lo que esta leyendo, o null. */
    public String getSystemId() {
        return this.systemId;
    }

    /** Nunca: siempre lleva un lector, porque el constructor no acepta null. */
    public boolean isEmpty() {
        return false;
    }
}
