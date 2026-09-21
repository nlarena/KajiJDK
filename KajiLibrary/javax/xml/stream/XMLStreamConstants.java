package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.XMLStreamConstants -- the fifteen event types a StAX reader can
 * return.
 *
 * <p>It is an interface without methods, and that form has a historical reason worth knowing:
 * {@link XMLStreamReader} <b>extends</b> it, so whoever programs against the reader writes
 * {@code reader.next() == START_ELEMENT} without importing anything. Today it would be written as a
 * constants class or an enum; in 2004 inheriting constants was the normal way of getting that.
 *
 * <p>The values are the contract's --1 to 15, in the order they are declared-- and there is code
 * that persists them, so they are not an internal detail.
 *
 * <h2>The ones a reader returns and the ones it does not</h2>
 *
 * <p>The list is longer than the states a normal walk goes through, and the difference matters:
 *
 * <ul>
 *   <li>{@link #START_DOCUMENT}, {@link #START_ELEMENT}, {@link #END_ELEMENT}, {@link #CHARACTERS},
 *       {@link #CDATA}, {@link #SPACE}, {@link #COMMENT}, {@link #PROCESSING_INSTRUCTION},
 *       {@link #ENTITY_REFERENCE}, {@link #DTD} and {@link #END_DOCUMENT} come out of
 *       {@link XMLStreamReader#next()};
 *   <li>{@link #ATTRIBUTE}, {@link #NAMESPACE}, {@link #NOTATION_DECLARATION} and {@link
 *       #ENTITY_DECLARATION} do <b>not</b>: they are {@link javax.xml.stream.events.XMLEvent} types
 *       that exist because the event model needs to name those things, but the cursor reader
 *       exposes them as properties of the element or of the DTD, not as steps of the walk.
 * </ul>
 */
public interface XMLStreamConstants {

    /** The start of an element: {@code <a>}. */
    int START_ELEMENT = 1;

    /** The end of an element: {@code </a>}. An {@code <a/>} produces both, start and end. */
    int END_ELEMENT = 2;

    /** A processing instruction: {@code <?target data?>}. */
    int PROCESSING_INSTRUCTION = 3;

    /**
     * Text.
     *
     * <p>The same piece of text can arrive split into several events if the factory does not have
     * {@code isCoalescing} set; that is not a whim but the consequence of the parser reading in
     * blocks.
     */
    int CHARACTERS = 4;

    /** A comment: {@code <!-- ... -->}. */
    int COMMENT = 5;

    /**
     * Ignorable whitespace.
     *
     * <p>It can only be told apart from ordinary text when there is a DTD or a schema saying that
     * element does not carry mixed content; without validation, the same space arrives as {@link
     * #CHARACTERS}.
     */
    int SPACE = 6;

    /**
     * The start of the document, before the root; it carries the version, the encoding and
     * standalone.
     */
    int START_DOCUMENT = 7;

    /** The end of the document; after this {@code hasNext()} gives false. */
    int END_DOCUMENT = 8;

    /** An entity reference that was not replaced: {@code &name;}. */
    int ENTITY_REFERENCE = 9;

    /** An attribute, as an event. The cursor reader does not return it; see the header. */
    int ATTRIBUTE = 10;

    /** The document type declaration: {@code <!DOCTYPE ...>}. */
    int DTD = 11;

    /**
     * A {@code <![CDATA[...]]>} section, when the factory does not merge it with the text next to
     * it.
     */
    int CDATA = 12;

    /** A namespace declaration, as an event. It does not come out of the cursor reader either. */
    int NAMESPACE = 13;

    /** A {@code <!NOTATION ...>} of the DTD. */
    int NOTATION_DECLARATION = 14;

    /** An {@code <!ENTITY ...>} of the DTD. */
    int ENTITY_DECLARATION = 15;
}
