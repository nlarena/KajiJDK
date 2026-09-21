package javax.xml.stream.events;

import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * KajiLibrary's javax.xml.stream.events.XMLEvent -- a piece of the document as an object of its
 * own, which can be kept.
 *
 * <h2>StAX's other model, and why there are two</h2>
 *
 * <p>{@link javax.xml.stream.XMLStreamReader} and this package are the two faces of StAX and solve
 * the same reading with opposite trade-offs. The cursor reader <b>is</b> the event: it creates no
 * objects, and in exchange nothing it returns survives the next {@code next()}. Here each event is
 * an immutable, independent object: it can be kept in a list, compared with another, or passed to
 * another thread.
 *
 * <p>The price is one object per event, which in a large document is exactly the cost the cursor
 * model exists to avoid. The practical rule: cursor when processing on the fly and discarding,
 * events when one has to look back --a mapping that needs the parent element, a buffer that is
 * reordered, a filter that decides later--.
 *
 * <h2>The ten {@code isXxx} and the three {@code asXxx}</h2>
 *
 * <p>The hierarchy uses disguised {@code instanceof}: {@link #isStartElement()} and company say
 * which subtype it is, and {@link #asStartElement()} does the downcast. It predates
 * pattern-matching {@code instanceof} in the language, and it is still the canonical way of walking
 * a {@link javax.xml.stream.XMLEventReader}.
 *
 * <p>{@link #getEventType()} returns the corresponding {@link XMLStreamConstants} constant, that
 * is, both models share the vocabulary of event types; that is what allows converting from one to
 * the other without translating anything.
 *
 * <h2>What is here</h2>
 *
 * <p>The complete interface. This library's implementations are returned both by {@link
 * javax.xml.stream.XMLEventFactory}, which builds events <b>from data the caller passes</b>, and by
 * the event reader of {@link javax.xml.stream.XMLInputFactory}, which builds them from reading a
 * document. (The note said no event comes from reading XML because there is no parser; there is one
 * now.)
 */
public interface XMLEvent extends XMLStreamConstants {

    /**
     * What kind of event it is, with the vocabulary of {@link XMLStreamConstants}.
     *
     * @return {@link XMLStreamConstants#START_ELEMENT}, {@link XMLStreamConstants#CHARACTERS}, etc.
     */
    int getEventType();

    /**
     * Where this event was in the document.
     *
     * <p>Unlike the cursor model, here the location is kept with the event, so it is still useful
     * after reading on.
     *
     * @return the location; it may be one without data, not null
     */
    Location getLocation();

    /**
     * Whether it is the start of an element.
     *
     * @return true if {@link #asStartElement()} is going to work
     */
    boolean isStartElement();

    /**
     * Whether it is an attribute.
     *
     * <p>An attribute is an event but does <b>not</b> appear in the stream: it comes hanging from
     * the {@link StartElement}. The type exists so that it can be treated as an event when needed.
     *
     * @return true if it is an {@link Attribute}
     */
    boolean isAttribute();

    /**
     * Whether it is a namespace declaration.
     *
     * @return true if it is a {@link Namespace}
     */
    boolean isNamespace();

    /**
     * Whether it is the end of an element.
     *
     * @return true if {@link #asEndElement()} is going to work
     */
    boolean isEndElement();

    /**
     * Whether it is an entity reference.
     *
     * @return true if it is an {@link EntityReference}
     */
    boolean isEntityReference();

    /**
     * Whether it is a processing instruction.
     *
     * @return true if it is a {@link ProcessingInstruction}
     */
    boolean isProcessingInstruction();

    /**
     * Whether it is text.
     *
     * <p>It also answers true for {@link XMLStreamConstants#CDATA} and {@link
     * XMLStreamConstants#SPACE}: the three are {@link Characters} and are told apart with {@link
     * Characters#isCData()} and {@link Characters#isIgnorableWhiteSpace()}.
     *
     * @return true if {@link #asCharacters()} is going to work
     */
    boolean isCharacters();

    /**
     * Whether it is the start of the document.
     *
     * @return true if it is a {@link StartDocument}
     */
    boolean isStartDocument();

    /**
     * Whether it is the end of the document.
     *
     * @return true if it is an {@link EndDocument}
     */
    boolean isEndDocument();

    /**
     * This event as the start of an element.
     *
     * @return the same object, with the more precise type
     * @throws ClassCastException if it is not the start of an element
     */
    StartElement asStartElement();

    /**
     * This event as the end of an element.
     *
     * @return the same object, with the more precise type
     * @throws ClassCastException if it is not the end of an element
     */
    EndElement asEndElement();

    /**
     * This event as text.
     *
     * @return the same object, with the more precise type
     * @throws ClassCastException if it is not text
     */
    Characters asCharacters();

    /**
     * The schema type of this event, if someone assigned it.
     *
     * <p>It exists for implementations that validate while reading and can annotate each event with
     * the type that corresponds to it. One that does not validate returns null, which is the normal
     * thing.
     *
     * @return the qualified name of the type, or null
     */
    QName getSchemaType();

    /**
     * Writes this event as XML.
     *
     * <p>It is the operation that makes a {@code List<XMLEvent>} a document: a loop that calls this
     * on each event writes the whole document. The text is escaped as appropriate for the event
     * type.
     *
     * @param writer where to write; cannot be null
     * @throws XMLStreamException if the writer fails
     */
    void writeAsEncodedUnicode(Writer writer) throws XMLStreamException;
}
