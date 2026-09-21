package javax.xml.stream;

import java.util.Iterator;

import javax.xml.stream.events.XMLEvent;

/**
 * KajiLibrary's javax.xml.stream.XMLEventReader -- StAX's other model: an iterator of events that
 * **can** be kept.
 *
 * <p>It does the same as {@link XMLStreamReader} --it pulls the document instead of having it
 * pushed-- but returns one {@link XMLEvent} at a time: a complete, immutable object with all its
 * information inside. The practical difference is a single one, and it decides which to use:
 *
 * <ul>
 *   <li>with the cursor, {@code getLocalName()} holds until the next {@code next()};
 *   <li>with events, the object can be kept in a list, compared with another further on and
 *       returned from a method.
 * </ul>
 *
 * <p>The price is one object per event. It is worth it when one has to look back --matching a start
 * with its end, gathering an element's children before deciding-- and it is not when each event is
 * processed and forgotten.
 *
 * <h2>An iterator with two faces</h2>
 *
 * <p>It extends {@link Iterator} of {@code Object} and not of {@code XMLEvent}, which is an
 * inheritance from when the interface was written without generics. The consequence is plain to
 * see: {@link #next()} returns {@code Object} and has to be cast, while {@link #nextEvent()}
 * returns the right type. They are the same advance, with two differences:
 *
 * <ul>
 *   <li>{@code nextEvent()} declares {@link XMLStreamException}, {@code next()} does not --it has
 *       to wrap it in an unchecked one--;
 *   <li>{@code next()} exists so that an enhanced {@code for} works, not because it is better.
 * </ul>
 *
 * <p>Always prefer {@code nextEvent()}: the read error arrives as what it is.
 *
 * <h2>What is written here</h2>
 *
 * <p>The seven methods. This package's implementation is {@code KajiEventReader}, which {@link
 * XMLInputFactory} returns, and there is also {@link javax.xml.stream.util.EventReaderDelegate},
 * the base class for wrapping someone else's and filtering or transforming it. (The note said there
 * is no implementation because this library comes with no XML parser; it has one now.)
 */
public interface XMLEventReader extends Iterator<Object> {

    /**
     * The next event.
     *
     * @return the event
     * @throws XMLStreamException if the document is malformed or reading fails
     * @throws java.util.NoSuchElementException if there are no more
     */
    XMLEvent nextEvent() throws XMLStreamException;

    /**
     * Whether at least one event remains.
     *
     * <p>Redeclared without {@code throws} because it comes from {@link Iterator}: a read error has
     * to come out through {@link #nextEvent()}, not here.
     *
     * @return true if there are more
     */
    boolean hasNext();

    /**
     * Looks at the next event **without** consuming it.
     *
     * <p>It is what the cursor model cannot give, and the commonest reason for choosing this model:
     * deciding what to do according to what comes, without having advanced yet.
     *
     * @return the next event, or null if there is none
     * @throws XMLStreamException if reading fails
     */
    XMLEvent peek() throws XMLStreamException;

    /**
     * The text of a text-only element, leaving the reader after its end.
     *
     * @return the text
     * @throws XMLStreamException if the current event is not a start or the element has children
     */
    String getElementText() throws XMLStreamException;

    /**
     * Skips whitespace, comments and processing instructions up to the next tag.
     *
     * @return the start or end event
     * @throws XMLStreamException if it finds something that is neither skippable nor a tag
     */
    XMLEvent nextTag() throws XMLStreamException;

    /**
     * The value of an implementation property.
     *
     * @param name the name of the property
     * @return the value
     * @throws IllegalArgumentException if the property does not exist
     */
    Object getProperty(String name) throws IllegalArgumentException;

    /**
     * Frees whatever the reader holds, without closing the source stream.
     *
     * @throws XMLStreamException if it fails
     */
    void close() throws XMLStreamException;
}
