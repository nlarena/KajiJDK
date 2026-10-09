package javax.xml.stream;

import java.util.NoSuchElementException;

import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventAllocator;

/**
 * This library's event reader: a cursor plus an {@link XMLEventAllocator}.
 *
 * <h2>The first event is already there</h2>
 *
 * <p>A newly built {@link XMLStreamReader} <b>is already standing</b> at {@code START_DOCUMENT}, so
 * the first {@link #nextEvent()} has to take a snapshot of where it is instead of advancing. Hence
 * {@code currentUndelivered}: it is the off-by-one between the two APIs, and it is the only
 * subtlety of this class.
 *
 * <h2>{@link #peek()}</h2>
 *
 * <p>Looking without consuming is what the cursor model cannot give, and it is solved in the only
 * possible way: the real event is asked for and kept. Since events are independent objects, keeping
 * it costs nothing; with the cursor all its state would have to be copied.
 */
final class KajiEventReader implements XMLEventReader {

    private final XMLStreamReader r;
    private final XMLEventAllocator alloc;
    private XMLEvent peeked;
    private boolean currentUndelivered = true;
    private XMLEvent last;

    KajiEventReader(XMLStreamReader r, XMLEventAllocator alloc) {
        this.r = r;
        this.alloc = alloc;
    }

    public boolean hasNext() {
        if (peeked != null || currentUndelivered) {
            return true;
        }
        try {
            return r.hasNext();
        } catch (XMLStreamException e) {
            return false;
        }
    }

    public XMLEvent nextEvent() throws XMLStreamException {
        if (peeked != null) {
            last = peeked;
            peeked = null;
            return last;
        }
        if (currentUndelivered) {
            currentUndelivered = false;
            last = alloc.allocate(r);
            return last;
        }
        if (!r.hasNext()) {
            throw new NoSuchElementException("no events left");
        }
        r.next();
        last = alloc.allocate(r);
        return last;
    }

    public XMLEvent peek() throws XMLStreamException {
        if (peeked != null) {
            return peeked;
        }
        if (!hasNext()) {
            return null;
        }
        XMLEvent previous = last;
        peeked = nextEvent();
        last = previous;
        return peeked;
    }

    public Object next() {
        try {
            return nextEvent();
        } catch (XMLStreamException e) {
            // The Iterator interface does not let a checked exception through, and losing the
            // reason would be worse than the change of type: it goes chained.
            NoSuchElementException n = new NoSuchElementException(e.getMessage());
            n.initCause(e);
            throw n;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("an event cannot be taken from an XML document");
    }

    public String getElementText() throws XMLStreamException {
        if (last == null || !last.isStartElement()) {
            throw new XMLStreamException(
                    "getElementText() is called after reading a START_ELEMENT");
        }
        StringBuilder sb = new StringBuilder();
        while (true) {
            XMLEvent e = nextEvent();
            if (e.isEndElement()) {
                return sb.toString();
            }
            if (e.isCharacters()) {
                sb.append(((Characters) e).getData());
                continue;
            }
            if (e.getEventType() == XMLStreamConstants.COMMENT
                    || e.getEventType() == XMLStreamConstants.PROCESSING_INSTRUCTION
                    || e.getEventType() == XMLStreamConstants.ENTITY_REFERENCE) {
                continue;
            }
            throw new XMLStreamException(
                    "the element is not text-only: got event " + e.getEventType(),
                    e.getLocation());
        }
    }

    public XMLEvent nextTag() throws XMLStreamException {
        while (true) {
            XMLEvent e = nextEvent();
            if (e.isStartElement() || e.isEndElement()) {
                return e;
            }
            if (e.isCharacters() && ((Characters) e).isWhiteSpace()) {
                continue;
            }
            if (e.getEventType() == XMLStreamConstants.COMMENT
                    || e.getEventType() == XMLStreamConstants.PROCESSING_INSTRUCTION
                    || e.getEventType() == XMLStreamConstants.START_DOCUMENT) {
                continue;
            }
            throw new XMLStreamException(
                    "expected a tag and got event " + e.getEventType(),
                    e.getLocation());
        }
    }

    public Object getProperty(String name) throws IllegalArgumentException {
        return r.getProperty(name);
    }

    public void close() throws XMLStreamException {
        r.close();
    }
}
