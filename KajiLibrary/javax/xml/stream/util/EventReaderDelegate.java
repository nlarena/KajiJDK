package javax.xml.stream.util;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * KajiLibrary's javax.xml.stream.util.EventReaderDelegate -- an event reader that forwards
 * everything to another.
 *
 * <p>The equivalent of {@link StreamReaderDelegate} for the other model, and for the same reason:
 * the subclass overrides the method it cares about and inherits the rest. Here there are seven
 * instead of forty, so the saving is smaller, but the symmetry between the two models is worth it
 * on its own.
 *
 * <p>As in the original, without a reader set any call blows up with {@link NullPointerException};
 * see the header of {@link StreamReaderDelegate}.
 */
public class EventReaderDelegate implements XMLEventReader {

    /** Whom everything is forwarded to. */
    private XMLEventReader reader;

    /**
     * A decorator without a reader yet.
     *
     * <p>{@link #setParent} has to be called before using it.
     */
    public EventReaderDelegate() {
    }

    /**
     * A decorator over the given reader.
     *
     * @param reader the underlying reader
     */
    public EventReaderDelegate(XMLEventReader reader) {
        this.reader = reader;
    }

    /**
     * Changes the underlying reader.
     *
     * @param reader the new reader
     */
    public void setParent(XMLEventReader reader) {
        this.reader = reader;
    }

    /**
     * The underlying reader.
     *
     * @return the reader, or null if it has not been set yet
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
