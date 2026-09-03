package javax.xml.stream.util;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * KajiLibrary's javax.xml.stream.util.EventReaderDelegate -- un lector de eventos que reenvia todo a
 * otro.
 *
 * <p>El equivalente de {@link StreamReaderDelegate} para el otro modelo, y por el mismo motivo: la
 * subclase redefine el metodo que le importa y hereda los demas. Aca son siete en vez de cuarenta,
 * asi que el ahorro es menor, pero la simetria entre los dos modelos vale por si sola.
 *
 * <p>Como el original, sin lector puesto cualquier llamada revienta con
 * {@link NullPointerException}; ver el encabezado de {@link StreamReaderDelegate}.
 */
public class EventReaderDelegate implements XMLEventReader {

    /** A quien se le reenvia todo. */
    private XMLEventReader reader;

    /**
     * Un decorador sin lector todavia.
     *
     * <p>Hay que llamar a {@link #setParent} antes de usarlo.
     */
    public EventReaderDelegate() {
    }

    /**
     * Un decorador sobre el lector dado.
     *
     * @param reader el lector de abajo
     */
    public EventReaderDelegate(XMLEventReader reader) {
        this.reader = reader;
    }

    /**
     * Cambia el lector de abajo.
     *
     * @param reader el lector nuevo
     */
    public void setParent(XMLEventReader reader) {
        this.reader = reader;
    }

    /**
     * El lector de abajo.
     *
     * @return el lector, o null si todavia no se puso
     */
    public XMLEventReader getParent() {
        return reader;
    }

    /** {@inheritDoc} */
    public XMLEvent nextEvent() throws XMLStreamException {
        return reader.nextEvent();
    }

    /** {@inheritDoc} */
    public boolean hasNext() {
        return reader.hasNext();
    }

    /** {@inheritDoc} */
    public XMLEvent peek() throws XMLStreamException {
        return reader.peek();
    }

    /** {@inheritDoc} */
    public void close() throws XMLStreamException {
        reader.close();
    }

    /** {@inheritDoc} */
    public String getElementText() throws XMLStreamException {
        return reader.getElementText();
    }

    /** {@inheritDoc} */
    public XMLEvent nextTag() throws XMLStreamException {
        return reader.nextTag();
    }

    /** {@inheritDoc} */
    public Object getProperty(String name) throws IllegalArgumentException {
        return reader.getProperty(name);
    }

    /** {@inheritDoc} */
    public Object next() {
        return reader.next();
    }

    /** {@inheritDoc} */
    public void remove() {
        reader.remove();
    }
}
