package javax.xml.stream;

import javax.xml.stream.util.StreamReaderDelegate;

/**
 * Un lector de cursor que solo se detiene donde el filtro deja.
 *
 * <p>Es {@link StreamReaderDelegate} con {@link #next()} redefinido, que es exactamente para lo que
 * esa clase existe.
 *
 * <p>La sutileza esta en el constructor: el lector de abajo puede estar parado en un evento que el
 * filtro rechaza --{@code START_DOCUMENT} lo es casi siempre-- asi que hay que avanzar hasta el
 * primero aceptado antes de devolver el objeto. Si no, la primera consulta al cursor, sin ningun
 * {@code next()} de por medio, contestaria sobre un evento filtrado.
 */
final class KajiFilteredStreamReader extends StreamReaderDelegate {

    private final StreamFilter filter;

    KajiFilteredStreamReader(XMLStreamReader r, StreamFilter filter) throws XMLStreamException {
        super(r);
        this.filter = filter;
        if (!filter.accept(r)) {
            advanceToAccepted();
        }
    }

    private int advanceToAccepted() throws XMLStreamException {
        while (super.hasNext()) {
            int t = super.next();
            if (filter.accept(this)) {
                return t;
            }
        }
        return super.getEventType();
    }

    public int next() throws XMLStreamException {
        return advanceToAccepted();
    }

    public int nextTag() throws XMLStreamException {
        int t = next();
        while (t != XMLStreamConstants.START_ELEMENT && t != XMLStreamConstants.END_ELEMENT
                && super.hasNext()) {
            t = next();
        }
        if (t != XMLStreamConstants.START_ELEMENT && t != XMLStreamConstants.END_ELEMENT) {
            throw new XMLStreamException("se esperaba una etiqueta y vino el evento " + t,
                    getLocation());
        }
        return t;
    }
}
