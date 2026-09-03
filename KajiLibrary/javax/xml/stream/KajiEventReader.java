package javax.xml.stream;

import java.util.NoSuchElementException;

import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventAllocator;

/**
 * El lector de eventos de esta biblioteca: un cursor mas un {@link XMLEventAllocator}.
 *
 * <h2>El primer evento ya esta puesto</h2>
 *
 * <p>Un {@link XMLStreamReader} recien construido <b>ya esta parado</b> en {@code START_DOCUMENT},
 * asi que el primer {@link #nextEvent()} tiene que fotografiar donde esta en vez de avanzar. De ahi
 * el {@code actualSinEntregar}: es el desfasaje de uno entre las dos APIs, y es la unica sutileza
 * de esta clase.
 *
 * <h2>{@link #peek()}</h2>
 *
 * <p>Mirar sin consumir es lo que el modelo de cursor no puede dar, y se resuelve de la unica forma
 * posible: se pide el evento de verdad y se guarda. Como los eventos son objetos independientes,
 * guardarlo no cuesta nada; con el cursor habria que copiar todo su estado.
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
            throw new NoSuchElementException("no quedan eventos");
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
            // La interfaz de Iterator no deja pasar una excepcion comprobada, y perder el motivo
            // seria peor que el cambio de tipo: va encadenada.
            NoSuchElementException n = new NoSuchElementException(e.getMessage());
            n.initCause(e);
            throw n;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("de un documento XML no se saca un evento");
    }

    public String getElementText() throws XMLStreamException {
        if (last == null || !last.isStartElement()) {
            throw new XMLStreamException(
                    "getElementText() se llama despues de haber leido un START_ELEMENT");
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
                    "el elemento no tiene solo texto: aparecio el evento " + e.getEventType(),
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
                    "se esperaba una etiqueta y vino el evento " + e.getEventType(),
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
