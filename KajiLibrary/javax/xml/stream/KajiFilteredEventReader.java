package javax.xml.stream;

import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * Un lector de eventos que solo entrega los que el filtro acepta.
 *
 * <p>Mas facil que su equivalente de cursor, porque un evento es un objeto: filtrar es descartarlo,
 * y {@link #peek()} se resuelve guardando el primero aceptado sin tener que deshacer nada.
 */
final class KajiFilteredEventReader extends EventReaderDelegate {

    private final EventFilter filter;
    private XMLEvent peeked;

    KajiFilteredEventReader(XMLEventReader r, EventFilter filter) {
        super(r);
        this.filter = filter;
    }

    public boolean hasNext() {
        if (peeked != null) {
            return true;
        }
        try {
            return advance() != null;
        } catch (XMLStreamException e) {
            return false;
        }
    }

    private XMLEvent advance() throws XMLStreamException {
        while (peeked == null && super.hasNext()) {
            XMLEvent e = super.nextEvent();
            if (filter.accept(e)) {
                peeked = e;
            }
        }
        return peeked;
    }

    public XMLEvent nextEvent() throws XMLStreamException {
        XMLEvent e = advance();
        if (e == null) {
            throw new java.util.NoSuchElementException("no quedan eventos aceptados");
        }
        peeked = null;
        return e;
    }

    public XMLEvent peek() throws XMLStreamException {
        return advance();
    }

    public Object next() {
        try {
            return nextEvent();
        } catch (XMLStreamException e) {
            java.util.NoSuchElementException n =
                    new java.util.NoSuchElementException(e.getMessage());
            n.initCause(e);
            throw n;
        }
    }
}
