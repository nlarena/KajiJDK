package javax.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventConsumer;

/**
 * KajiLibrary's javax.xml.stream.XMLEventWriter -- writing XML by handing over event objects
 * instead of calling one method per piece.
 *
 * <p>It is to {@link XMLStreamWriter} what {@link XMLEventReader} is to {@link XMLStreamReader}.
 * The practical advantage is in {@link #add(XMLEventReader)}: plugging an event reader into a
 * writer copies a whole document in one line, and with an {@link EventFilter} in between it is
 * filtered without writing the loop. That cannot be done with the cursor model because a cursor
 * event is not an object that can be passed from hand to hand.
 *
 * <p>It extends {@link XMLEventConsumer}, which is what lets a {@link
 * javax.xml.stream.util.XMLEventAllocator} write straight here without knowing what is on the other
 * side.
 *
 * <h2>What is written here</h2>
 *
 * <p>The nine methods. This package's implementation is {@code KajiEventWriter}, which {@link
 * XMLOutputFactory} returns. (The note said there is no implementation because this library has no
 * StAX provider; it has one now.)
 */
public interface XMLEventWriter extends XMLEventConsumer {

    /**
     * Flushes whatever is in the buffer to the destination.
     *
     * @throws XMLStreamException if it fails to write
     */
    void flush() throws XMLStreamException;

    /**
     * Frees whatever the writer holds, without closing the destination stream.
     *
     * @throws XMLStreamException if it fails
     */
    void close() throws XMLStreamException;

    /**
     * Writes an event.
     *
     * @param event the event
     * @throws XMLStreamException if it fails to write
     */
    void add(XMLEvent event) throws XMLStreamException;

    /**
     * Writes everything left in an event reader, and leaves it empty.
     *
     * <p>The one-line copy of a document; see the header.
     *
     * @param reader where to take the events from
     * @throws XMLStreamException if reading or writing fails
     */
    void add(XMLEventReader reader) throws XMLStreamException;

    /**
     * The prefix bound to a namespace, or null.
     *
     * @param uri the namespace
     * @return the prefix
     * @throws XMLStreamException if it fails
     */
    String getPrefix(String uri) throws XMLStreamException;

    /**
     * Binds a prefix to a namespace for whatever is written from here on.
     *
     * <p>As in {@link XMLStreamWriter#setPrefix}, it does not write the declaration.
     *
     * @param prefix the prefix
     * @param uri the namespace
     * @throws XMLStreamException if it fails
     */
    void setPrefix(String prefix, String uri) throws XMLStreamException;

    /**
     * Binds the default namespace, without writing the declaration.
     *
     * @param uri the namespace
     * @throws XMLStreamException if it fails
     */
    void setDefaultNamespace(String uri) throws XMLStreamException;

    /**
     * Replaces the namespace context, only before the root element.
     *
     * @param context the new context
     * @throws XMLStreamException if it fails or if it is too late
     */
    void setNamespaceContext(NamespaceContext context) throws XMLStreamException;

    /**
     * The bindings in force.
     *
     * @return the context
     */
    NamespaceContext getNamespaceContext();
}
