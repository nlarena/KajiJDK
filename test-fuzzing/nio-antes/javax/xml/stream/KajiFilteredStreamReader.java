package javax.xml.stream;

import javax.xml.stream.util.StreamReaderDelegate;

/**
 * A cursor reader that only stops where the filter lets it.
 *
 * <p>It is {@link StreamReaderDelegate} with {@link #next()} redefined, which is exactly what that
 * class exists for.
 *
 * <p>The subtlety is in the constructor: the underlying reader may be standing on an event the
 * filter rejects --{@code START_DOCUMENT} almost always is-- so it has to advance to the first
 * accepted one before returning the object. Otherwise, the first query to the cursor, without any
 * {@code next()} in between, would answer about a filtered event.
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
            throw new XMLStreamException("expected a tag and got event " + t,
                    getLocation());
        }
        return t;
    }
}
