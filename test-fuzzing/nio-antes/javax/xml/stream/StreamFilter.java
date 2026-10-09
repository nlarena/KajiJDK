package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.StreamFilter -- the criterion with which {@link
 * XMLInputFactory#createFilteredReader(XMLStreamReader, StreamFilter)} decides which events to let
 * through.
 *
 * <p>Filtering on the pulling side is StAX's concrete advantage over SAX: the filtered reader
 * **skips** the rejected events without returning them, so the application's code never sees what
 * it did not ask for and does not pay to discard it either. In SAX the filter has to be in the
 * handler, which is called for everything anyway.
 *
 * <p>The filter is called with the reader **standing** on the event to judge, not with a copy: it
 * can look at {@code getLocalName()}, the attributes, the depth. That is what makes it useful and
 * what forces the only rule it has: <b>it cannot advance the reader</b>. An {@code accept} that
 * calls {@code next()} eats events from the outer walk and the result depends on when the filter is
 * evaluated, which is the definition of a bug that does not reproduce.
 */
public interface StreamFilter {

    /**
     * Decides whether the event the reader is standing on is let through.
     *
     * @param reader the reader, positioned on the event to judge; it must not be advanced
     * @return true for the event to reach the caller, false to skip it
     */
    boolean accept(XMLStreamReader reader);
}
