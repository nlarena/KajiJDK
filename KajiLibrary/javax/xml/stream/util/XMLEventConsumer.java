package javax.xml.stream.util;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * KajiLibrary's javax.xml.stream.util.XMLEventConsumer -- anything that knows how to receive an
 * event.
 *
 * <p>A single-method interface, and that is the whole idea: separating "something events can be
 * given to" from "something that writes XML". {@link javax.xml.stream.XMLEventWriter} extends it
 * and is the obvious implementation, but a buffer that gathers events in a list, a filter that
 * forwards some, or a validator that looks at them as they go by are also consumers and write
 * nothing.
 *
 * <p>It is what lets {@link XMLEventAllocator#allocate(javax.xml.stream.XMLStreamReader,
 * XMLEventConsumer)} deliver the event without knowing where it goes.
 */
public interface XMLEventConsumer {

    /**
     * Receives an event.
     *
     * @param event the event; whether a null is accepted depends on the implementation
     * @throws XMLStreamException if the consumer cannot accept it
     */
    void add(XMLEvent event) throws XMLStreamException;
}
