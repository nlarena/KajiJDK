package javax.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.stream.XMLStreamReader -- StAX's cursor model: the one that **pulls** the
 * document instead of having it pushed.
 *
 * <p>It is the underlying difference from SAX and the reason for the whole package. In SAX the
 * parser is in charge: it calls the handler's methods when it wants, and the application, if it
 * needs to remember where it was, has to note it down in fields --a state machine written by hand
 * for each use--. Here the application is in charge: it calls {@link #next()} when it is ready, and
 * the position in the document is the position in its own code. A {@code while (r.hasNext())} loop
 * with a {@code switch} inside replaces the whole handler.
 *
 * <p>That also makes possible two things that cost a lot in SAX: **stopping** halfway --and not
 * reading the rest-- and **combining** two documents by reading a bit of each.
 *
 * <h2>A single object whose content changes</h2>
 *
 * <p>The rule to keep in mind: the reader **is** the event. {@code getLocalName()} does not return
 * the name of "an" element but that of the element the cursor is standing on now, and after the
 * next {@code next()} it returns something else. Nothing that comes out of here --not even the
 * {@code char[]} of {@link #getTextCharacters()}-- can be kept for later.
 *
 * <p>From there comes the whole performance gain of the cursor model: zero objects per event. And
 * from there also comes the existence of the other model, {@link XMLEventReader}, for when what is
 * needed is precisely to keep.
 *
 * <p>The other consequence, less obvious: **each method is valid only in certain states**. Asking
 * for {@link #getName()} while standing on a {@link XMLStreamConstants#CHARACTERS} is a caller
 * error and raises {@link IllegalStateException}, not null. Each method says below where it is
 * valid.
 *
 * <h2>What is written here</h2>
 *
 * <p>The interface is complete: the forty-five methods, with their valid states and their
 * exceptions. This package's implementation is {@code KajiStreamReader}, an XML 1.0 parser of its
 * own --tokenizer, entity handling, namespace resolution, decoding according to the XML
 * declaration--; see {@link XMLInputFactory} for what it does and does not do. (The note said there
 * is no implementation here, because a real reader is a whole XML parser; there is one now.)
 *
 * <p>What should still be avoided is the opposite temptation to a factory's: a factory that finds
 * no parser can fail honestly, but a reader that returned invented events would lie to the caller
 * without anything breaking. A document that was never read and still produced events is the worst
 * possible result, so there is no fake reader in this library.
 */
public interface XMLStreamReader extends XMLStreamConstants {

    /**
     * The value of an implementation property.
     *
     * @param name the name of the property; cannot be null
     * @return the value
     * @throws IllegalArgumentException if {@code name} is null
     */
    Object getProperty(String name) throws IllegalArgumentException;

    /**
     * Advances to the next event and returns its type.
     *
     * <p>This is the method that defines the model: nothing happens until the caller asks for it.
     *
     * @return one of the types of {@link XMLStreamConstants}
     * @throws XMLStreamException if the document is malformed or reading fails
     * @throws java.util.NoSuchElementException if there are no more events
     */
    int next() throws XMLStreamException;

    /**
     * Checks that the cursor is where the caller thinks, and if not, cuts.
     *
     * <p>It is an assertion in the shape of a method, and it serves so that a structure error jumps
     * out at the place where it was noticed and not five events later, when nobody knows where it
     * came from. A null in either of the last two parameters means "I do not care about this one".
     *
     * @param type the expected event type
     * @param namespaceURI the expected namespace, or null not to check it
     * @param localName the expected local name, or null not to check it
     * @throws XMLStreamException if the current event does not match
     */
    void require(int type, String namespaceURI, String localName) throws XMLStreamException;

    /**
     * The text of an element that only contains text, leaving the cursor at its end.
     *
     * <p>A shortcut for the most common case of all --{@code <price>12.50</price>}-- which without
     * this is five lines of loop. It fails if the element has children, which is precisely what
     * makes it worthwhile: it does not return the text of an element with structure as if it had
     * none.
     *
     * <p>Valid only standing on a {@link XMLStreamConstants#START_ELEMENT}.
     *
     * @return the text between the start and the end
     * @throws XMLStreamException if the cursor is not at a start or the element is not text-only
     */
    String getElementText() throws XMLStreamException;

    /**
     * Skips whitespace, comments and processing instructions up to the next tag.
     *
     * <p>What makes walking an indented document readable: without this, each line break of the
     * file is a text event that has to be discarded by hand.
     *
     * @return {@link XMLStreamConstants#START_ELEMENT} or {@link XMLStreamConstants#END_ELEMENT}
     * @throws XMLStreamException if it finds something that is neither skippable nor a tag
     */
    int nextTag() throws XMLStreamException;

    /**
     * Whether at least one event remains to be read.
     *
     * @return true if {@link #next()} has something to return
     * @throws XMLStreamException if reading fails
     */
    boolean hasNext() throws XMLStreamException;

    /**
     * Frees whatever the reader holds.
     *
     * <p>It does not close the source {@link java.io.InputStream} nor {@link java.io.Reader}:
     * whoever opened it closes it. That rule avoids a reader closing the stream underneath someone
     * who was sharing it.
     *
     * @throws XMLStreamException if it fails
     */
    void close() throws XMLStreamException;

    /**
     * The namespace bound to a prefix at the current position.
     *
     * @param prefix the prefix; the empty string asks for the default one
     * @return the URI, or null if the prefix is not bound
     */
    String getNamespaceURI(String prefix);

    /**
     * Whether the cursor is at the start of an element.
     *
     * @return true if the current event is {@link XMLStreamConstants#START_ELEMENT}
     */
    boolean isStartElement();

    /**
     * Whether the cursor is at the end of an element.
     *
     * @return true if the current event is {@link XMLStreamConstants#END_ELEMENT}
     */
    boolean isEndElement();

    /**
     * Whether the cursor is at text.
     *
     * @return true if the current event is {@link XMLStreamConstants#CHARACTERS}
     */
    boolean isCharacters();

    /**
     * Whether the current event is text and it is all whitespace.
     *
     * @return true if it is space
     */
    boolean isWhiteSpace();

    /**
     * The value of an attribute of the current element, looked up by name.
     *
     * <p>Valid at {@link XMLStreamConstants#START_ELEMENT} and {@link
     * XMLStreamConstants#ATTRIBUTE}.
     *
     * @param namespaceURI the namespace of the attribute, or null not to look at it
     * @param localName the local name of the attribute
     * @return the value, or null if the attribute is not there
     * @throws IllegalStateException if the cursor is not in a state where there are attributes
     */
    String getAttributeValue(String namespaceURI, String localName);

    /**
     * How many attributes the current element has.
     *
     * <p>It does not count the namespace declarations: {@code xmlns:a="..."} is not an attribute
     * for these purposes, and that is why there is a separate {@link #getNamespaceCount()}.
     *
     * @return the count
     * @throws IllegalStateException if the cursor is not in a state where there are attributes
     */
    int getAttributeCount();

    /**
     * The qualified name of attribute number {@code index}.
     *
     * @param index the index, from 0
     * @return the name
     * @throws IllegalStateException if the cursor is not in a state where there are attributes
     */
    QName getAttributeName(int index);

    /**
     * The namespace of attribute number {@code index}, or null if it has none.
     *
     * @param index the index, from 0
     * @return the namespace
     * @throws IllegalStateException if the cursor is not in a state where there are attributes
     */
    String getAttributeNamespace(int index);

    /**
     * The local name of attribute number {@code index}.
     *
     * @param index the index, from 0
     * @return the local name
     * @throws IllegalStateException if the cursor is not in a state where there are attributes
     */
    String getAttributeLocalName(int index);

    /**
     * The prefix of attribute number {@code index}, or null if it has none.
     *
     * @param index the index, from 0
     * @return the prefix
     * @throws IllegalStateException if the cursor is not in a state where there are attributes
     */
    String getAttributePrefix(int index);

    /**
     * The declared type of the attribute --{@code CDATA}, {@code ID}, {@code IDREF}...-- according
     * to the DTD.
     *
     * <p>Without a DTD they are all {@code CDATA}, which is the same as saying "text and nothing
     * more".
     *
     * @param index the index, from 0
     * @return the type
     * @throws IllegalStateException if the cursor is not in a state where there are attributes
     */
    String getAttributeType(int index);

    /**
     * The value of attribute number {@code index}.
     *
     * @param index the index, from 0
     * @return the value
     * @throws IllegalStateException if the cursor is not in a state where there are attributes
     */
    String getAttributeValue(int index);

    /**
     * Whether the attribute was written in the document or the DTD put it there by default.
     *
     * <p>The distinction matters for writing the document back: an attribute that came from a
     * default value need not be written, and writing it changes the document.
     *
     * @param index the index, from 0
     * @return true if it was written
     * @throws IllegalStateException if the cursor is not in a state where there are attributes
     */
    boolean isAttributeSpecified(int index);

    /**
     * How many namespace declarations there are in this event.
     *
     * <p>Only the ones declared **in this element**, not the inherited ones: for the inherited ones
     * there is {@link #getNamespaceContext()}.
     *
     * <p>Valid at {@link XMLStreamConstants#START_ELEMENT}, {@link XMLStreamConstants#END_ELEMENT}
     * and {@link XMLStreamConstants#NAMESPACE}.
     *
     * @return the count
     * @throws IllegalStateException if the cursor is not in one of those states
     */
    int getNamespaceCount();

    /**
     * The prefix of declaration number {@code index}, or null if it is the default namespace's.
     *
     * @param index the index, from 0
     * @return the prefix
     * @throws IllegalStateException if the cursor is not in a state with declarations
     */
    String getNamespacePrefix(int index);

    /**
     * The URI of declaration number {@code index}.
     *
     * @param index the index, from 0
     * @return the namespace
     * @throws IllegalStateException if the cursor is not in a state with declarations
     */
    String getNamespaceURI(int index);

    /**
     * The prefix/URI bindings in force at the current position, inherited ones included.
     *
     * <p>The context belongs to the reader, not to the event: it changes as the cursor advances,
     * and it cannot be kept to be queried later.
     *
     * @return the context
     */
    NamespaceContext getNamespaceContext();

    /**
     * The type of the current event.
     *
     * @return one of the types of {@link XMLStreamConstants}
     */
    int getEventType();

    /**
     * The text of the current event, as a {@link String}.
     *
     * <p>Valid at text, CDATA, comment, space, entity reference and DTD.
     *
     * @return the text
     * @throws IllegalStateException if the current event carries no text
     */
    String getText();

    /**
     * The same text, without copying it into a {@link String}.
     *
     * <p>The array is **the reader's** and holds until the next {@link #next()}: it has to be read
     * between {@link #getTextStart()} and {@link #getTextLength()}, not whole, and it must not be
     * kept. It exists for code that processes large text and does not want to allocate a string per
     * event.
     *
     * @return the internal buffer
     * @throws IllegalStateException if the current event carries no text
     */
    char[] getTextCharacters();

    /**
     * Copies part of the text into the caller's array.
     *
     * <p>The safe variant of {@link #getTextCharacters()}: what is copied belongs to the caller and
     * lasts as long as they want.
     *
     * @param sourceStart from which character of the text
     * @param target where to copy
     * @param targetStart from which position of the target
     * @param length how many characters at most
     * @return how many were copied
     * @throws XMLStreamException if reading fails
     * @throws IndexOutOfBoundsException if the indices do not fit in the target
     * @throws IllegalStateException if the current event carries no text
     */
    int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
            throws XMLStreamException;

    /**
     * From which position of the {@link #getTextCharacters()} array the text starts.
     *
     * @return the offset
     * @throws IllegalStateException if the current event carries no text
     */
    int getTextStart();

    /**
     * How many characters of the {@link #getTextCharacters()} array are the text.
     *
     * @return the length
     * @throws IllegalStateException if the current event carries no text
     */
    int getTextLength();

    /**
     * The encoding of the document, if it could be determined.
     *
     * <p>It is the one the parser **deduced or was told**, and it may not be the XML declaration's:
     * for that one there is {@link #getCharacterEncodingScheme()}.
     *
     * @return the name of the encoding, or null
     */
    String getEncoding();

    /**
     * Whether the current event carries text.
     *
     * @return true at text, CDATA, comment, space, entity reference and DTD
     */
    boolean hasText();

    /**
     * Where, in the input, the current event is.
     *
     * @return the location; never null, though it may have no numbers
     */
    Location getLocation();

    /**
     * The qualified name of the current element.
     *
     * <p>Valid only at the start and end of an element; in any other state it is a caller error.
     *
     * @return the name
     * @throws IllegalStateException if the current event has no name
     */
    QName getName();

    /**
     * The local name of the current element, or the entity's name in a reference.
     *
     * @return the local name
     * @throws IllegalStateException if the current event has no name
     */
    String getLocalName();

    /**
     * Whether the current event has a name.
     *
     * @return true at the start and end of an element
     */
    boolean hasName();

    /**
     * The namespace of the current element, or null if it has none.
     *
     * @return the namespace
     */
    String getNamespaceURI();

    /**
     * The prefix of the current element, or null if it has none.
     *
     * @return the prefix
     */
    String getPrefix();

    /**
     * The version declared in the XML declaration, or null if there was none.
     *
     * @return the version
     */
    String getVersion();

    /**
     * The value of {@code standalone} of the XML declaration.
     *
     * <p>It returns false both if it said {@code no} and if there was no declaration; to tell them
     * apart one has to ask {@link #standaloneSet()}.
     *
     * @return true if the document was declared standalone
     */
    boolean isStandalone();

    /**
     * Whether the XML declaration carried {@code standalone}.
     *
     * @return true if it was written
     */
    boolean standaloneSet();

    /**
     * The encoding **declared** in the XML declaration, or null if there was none.
     *
     * @return the name of the encoding
     */
    String getCharacterEncodingScheme();

    /**
     * The target of the current processing instruction.
     *
     * @return the target, or null if the event is not a processing instruction
     */
    String getPITarget();

    /**
     * The data of the current processing instruction.
     *
     * @return the data, or null if the event is not a processing instruction
     */
    String getPIData();
}
