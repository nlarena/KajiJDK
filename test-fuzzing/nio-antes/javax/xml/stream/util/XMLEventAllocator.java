package javax.xml.stream.util;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

/**
 * KajiLibrary's javax.xml.stream.util.XMLEventAllocator -- what turns a cursor's position into an
 * event.
 *
 * <h2>The extension point between the two models</h2>
 *
 * <p>A {@link javax.xml.stream.XMLEventReader} of this library --and of almost all-- is an {@link
 * XMLStreamReader} plus this: the cursor advances and the allocator takes a snapshot of each
 * position in an independent object. Its being replaceable ({@link
 * javax.xml.stream.XMLInputFactory#ALLOCATOR}) allows returning event implementations of one's own
 * --with extra fields, or cheaper-- without touching the parser.
 *
 * <h2>{@link #newInstance()} is an instance method, and that is fine</h2>
 *
 * <p>It is surprising that the way to get an allocator is to ask another allocator for it. The
 * reason is that the factory receives <b>one</b> instance through configuration and each reader
 * needs its own, because an allocator can have state --name tables, reused buffers--. A static
 * method would not do: the factory does not know the class, it only has the object. That is, the
 * configured instance works as a prototype.
 */
public interface XMLEventAllocator {

    /**
     * Another allocator of the same class, for a new reader.
     *
     * <p>The one receiving the call acts as prototype; see the header.
     *
     * @return a new allocator, not sharing state with this one
     */
    XMLEventAllocator newInstance();

    /**
     * The event corresponding to the cursor's current position.
     *
     * <p>It does not advance the reader: it reads it where it is.
     *
     * @param reader the cursor, standing on an event
     * @return the event as an object of its own
     * @throws XMLStreamException if the reader fails when queried
     */
    XMLEvent allocate(XMLStreamReader reader) throws XMLStreamException;

    /**
     * The same, but handing it to a consumer instead of returning it.
     *
     * <p>The variant exists for the cases where one position of the cursor gives <b>more than
     * one</b> event --an allocator that decides to split a long text, for example--, which the one
     * that returns a single event cannot express.
     *
     * @param reader the cursor, standing on an event
     * @param consumer whom to give what comes out to
     * @throws XMLStreamException if the reader or the consumer fails
     */
    void allocate(XMLStreamReader reader, XMLEventConsumer consumer) throws XMLStreamException;
}
