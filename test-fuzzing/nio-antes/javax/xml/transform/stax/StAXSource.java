package javax.xml.transform.stax;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;

/**
 * KajiLibrary's javax.xml.transform.stax.StAXSource -- a StAX reader as the source of a
 * transformation.
 *
 * <p>It carries <b>one</b> of the two StAX readers, the stream one or the event one, and never
 * both: the getter of the one that was not passed returns null. It is so because both are the same
 * reading seen two ways, and converting one into the other would consume the one received.
 *
 * <h2>The reader has to be at the start of something</h2>
 *
 * <p>The constructor requires the reader to be standing at {@code START_DOCUMENT} or {@code
 * START_ELEMENT}, and if not, it throws. The validation is worth it: a half-consumed reader would
 * produce a transformation of an arbitrary fragment, and the error would only appear much later,
 * when the output comes out cut short.
 *
 * <p>That it accepts {@code START_ELEMENT} and not only {@code START_DOCUMENT} is on purpose: it
 * lets a <b>subtree</b> of a large document be transformed without taking it out separately.
 *
 * <h2>The system identifier is read-only</h2>
 *
 * <p>{@link #setSystemId} throws {@link UnsupportedOperationException}, which is odd for a setter
 * and is right: the identifier comes from the reader's position, which is what knows where what it
 * is reading came from. Letting it be changed would allow lying about the origin, and relative
 * references would be resolved against a false base.
 */
public class StAXSource implements Source {

    /** With this a {@code TransformerFactory} is asked whether it accepts this source. */
    public static final String FEATURE = "http://javax.xml.transform.stax.StAXSource/feature";

    /** One of the two is null; see the class note. */
    private XMLStreamReader streamReader;

    private XMLEventReader eventReader;

    /** The reader's, cached when constructing. */
    private String systemId;

    /**
     * With an event reader.
     *
     * @throws IllegalArgumentException if it is null
     * @throws IllegalStateException if it is not at the start of a document or of an element
     * @throws XMLStreamException if looking at its next event fails. (The note gave this exception
     *     for a reader not at a start; the code, like the JDK, throws {@code IllegalStateException}
     *     there.)
     */
    public StAXSource(XMLEventReader reader) throws XMLStreamException {
        if (reader == null) {
            throw new IllegalArgumentException(
                "StAXSource(XMLEventReader) with XMLEventReader == null");
        }
        // `peek` and not `nextEvent`: looking must not consume, because whoever receives it has to
        // be able to read from the beginning.
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
     * With a stream reader.
     *
     * @throws IllegalArgumentException if it is null
     * @throws IllegalStateException if it is not at the start of a document or of an element
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

    /** The event reader, or null if it was built with the stream one. */
    public XMLEventReader getXMLEventReader() {
        return this.eventReader;
    }

    /** The stream reader, or null if it was built with the event one. */
    public XMLStreamReader getXMLStreamReader() {
        return this.streamReader;
    }

    /**
     * Cannot be changed.
     *
     * @throws UnsupportedOperationException always; see the class note
     */
    public void setSystemId(String systemId) {
        throw new UnsupportedOperationException(
            "StAXSource#setSystemId(systemId) cannot set the system identifier for a StAXSource");
    }

    /** Where the reader says what it is reading comes from, or null. */
    public String getSystemId() {
        return this.systemId;
    }

    /** Never: it always carries a reader, because the constructor does not accept null. */
    public boolean isEmpty() {
        return false;
    }
}
