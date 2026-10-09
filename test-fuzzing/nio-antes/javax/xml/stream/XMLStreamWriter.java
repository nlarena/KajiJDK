package javax.xml.stream;

import javax.xml.namespace.NamespaceContext;

/**
 * KajiLibrary's javax.xml.stream.XMLStreamWriter -- writing XML by calling one method per piece,
 * without building a tree.
 *
 * <p>It is the reverse of {@link XMLStreamReader} and has the same virtue: the document is written
 * as it is generated, so memory does not grow with the size of the output. A catalog of a million
 * products is written with a loop; with DOM the million would have to be in memory before writing
 * the first byte.
 *
 * <h2>What the writer does not do for you</h2>
 *
 * <p>This is what surprises most about the interface, and it is an explicit design decision: <b>the
 * writer does not check that the document comes out well-formed</b>. Nothing forces each {@code
 * writeStartElement} to have its {@code writeEndElement}, and a typical implementation does not
 * keep count. The reason is the usual one in StAX: keeping it costs on the hot path, and whoever
 * generates the document already knows what structure they are generating.
 *
 * <p>What it does do, and it has to be kept in mind, is **escape** the text: {@link
 * #writeCharacters} turns {@code &} into {@code &amp;} and {@code <} into {@code &lt;}. That is why
 * there are separate methods for text and for markup, and why there is no "write this raw".
 *
 * <h2>Prefixes, and repairing mode</h2>
 *
 * <p>Namespaces are the heavy part of writing XML: the prefix has to be declared before using it
 * and the declaration must not be repeated in every element. The interface gives both ways of
 * handling it:
 *
 * <ul>
 *   <li>by hand, with {@link #setPrefix} and {@link #writeNamespace}, which is what has to be done
 *       by default;
 *   <li>automatic, if the factory has {@link XMLOutputFactory#IS_REPAIRING_NAMESPACES} set: the
 *       writer invents and declares the prefixes that are needed.
 * </ul>
 *
 * <p>{@link #setPrefix} has a subtlety that is paid for dearly if overlooked: **it declares the
 * intention, it writes nothing**. The declaration only comes out with {@link #writeNamespace}.
 * Calling only the first produces a document with undeclared prefixes, which is a broken document.
 *
 * <h2>What is written here</h2>
 *
 * <p>The thirty-two methods of the interface. This package's implementation is {@code
 * KajiStreamWriter}, which {@link XMLOutputFactory} returns. (The note said there is no
 * implementation because this library has no StAX provider; it has one now.)
 */
public interface XMLStreamWriter {

    /**
     * Opens an unqualified element.
     *
     * @param localName the name
     * @throws XMLStreamException if it fails to write
     */
    void writeStartElement(String localName) throws XMLStreamException;

    /**
     * Opens an element in a namespace, with the prefix that corresponds by context.
     *
     * @param namespaceURI the namespace
     * @param localName the local name
     * @throws XMLStreamException if it fails to write
     */
    void writeStartElement(String namespaceURI, String localName) throws XMLStreamException;

    /**
     * Opens an element with explicit prefix, namespace and local name.
     *
     * @param prefix the prefix
     * @param localName the local name
     * @param namespaceURI the namespace
     * @throws XMLStreamException if it fails to write
     */
    void writeStartElement(String prefix, String localName, String namespaceURI)
            throws XMLStreamException;

    /**
     * Writes an empty element, {@code <a/>}, in a namespace.
     *
     * <p>It does not have to be closed: it opens no level.
     *
     * @param namespaceURI the namespace
     * @param localName the local name
     * @throws XMLStreamException if it fails to write
     */
    void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException;

    /**
     * Writes an empty element with an explicit prefix.
     *
     * @param prefix the prefix
     * @param localName the local name
     * @param namespaceURI the namespace
     * @throws XMLStreamException if it fails to write
     */
    void writeEmptyElement(String prefix, String localName, String namespaceURI)
            throws XMLStreamException;

    /**
     * Writes an unqualified empty element.
     *
     * @param localName the name
     * @throws XMLStreamException if it fails to write
     */
    void writeEmptyElement(String localName) throws XMLStreamException;

    /**
     * Closes the most recent open element.
     *
     * @throws XMLStreamException if it fails to write
     */
    void writeEndElement() throws XMLStreamException;

    /**
     * Closes all the elements left open and finishes the document.
     *
     * @throws XMLStreamException if it fails to write
     */
    void writeEndDocument() throws XMLStreamException;

    /**
     * Frees whatever the writer holds.
     *
     * <p>It does not close the destination stream --whoever opened it closes it-- and it does
     * **not** flush what is pending: for that there is {@link #flush()}.
     *
     * @throws XMLStreamException if it fails
     */
    void close() throws XMLStreamException;

    /**
     * Flushes whatever is in the buffer to the destination.
     *
     * @throws XMLStreamException if it fails to write
     */
    void flush() throws XMLStreamException;

    /**
     * Writes an unqualified attribute in the open element.
     *
     * @param localName the name
     * @param value the value, which is escaped
     * @throws XMLStreamException if there is no open element or it fails to write
     */
    void writeAttribute(String localName, String value) throws XMLStreamException;

    /**
     * Writes an attribute with explicit prefix and namespace.
     *
     * @param prefix the prefix
     * @param namespaceURI the namespace
     * @param localName the local name
     * @param value the value, which is escaped
     * @throws XMLStreamException if there is no open element or it fails to write
     */
    void writeAttribute(String prefix, String namespaceURI, String localName, String value)
            throws XMLStreamException;

    /**
     * Writes an attribute in a namespace, with the prefix that corresponds by context.
     *
     * @param namespaceURI the namespace
     * @param localName the local name
     * @param value the value, which is escaped
     * @throws XMLStreamException if there is no open element or it fails to write
     */
    void writeAttribute(String namespaceURI, String localName, String value)
            throws XMLStreamException;

    /**
     * Writes an {@code xmlns:prefix="uri"} declaration in the open element.
     *
     * @param prefix the prefix to declare
     * @param namespaceURI the namespace
     * @throws XMLStreamException if there is no open element or it fails to write
     */
    void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException;

    /**
     * Writes the declaration of the default namespace, {@code xmlns="uri"}.
     *
     * @param namespaceURI the namespace
     * @throws XMLStreamException if there is no open element or it fails to write
     */
    void writeDefaultNamespace(String namespaceURI) throws XMLStreamException;

    /**
     * Writes a comment.
     *
     * @param data the content, without the delimiters
     * @throws XMLStreamException if it fails to write
     */
    void writeComment(String data) throws XMLStreamException;

    /**
     * Writes a processing instruction without data.
     *
     * @param target the target
     * @throws XMLStreamException if it fails to write
     */
    void writeProcessingInstruction(String target) throws XMLStreamException;

    /**
     * Writes a processing instruction with data.
     *
     * @param target the target
     * @param data the data
     * @throws XMLStreamException if it fails to write
     */
    void writeProcessingInstruction(String target, String data) throws XMLStreamException;

    /**
     * Writes a CDATA section.
     *
     * <p>The difference from {@link #writeCharacters}: the content goes **unescaped**, between
     * {@code <![CDATA[} and {@code ]]>}. It serves for putting in text full of {@code <} --source
     * code, embedded XML-- without it becoming unreadable.
     *
     * @param data the content
     * @throws XMLStreamException if it fails to write
     */
    void writeCData(String data) throws XMLStreamException;

    /**
     * Writes a whole document type declaration, as given.
     *
     * @param dtd the complete text of the {@code <!DOCTYPE ...>}
     * @throws XMLStreamException if it fails to write
     */
    void writeDTD(String dtd) throws XMLStreamException;

    /**
     * Writes an entity reference: {@code &name;}.
     *
     * @param name the name of the entity, without the ampersand nor
     *     the semicolon
     * @throws XMLStreamException if it fails to write
     */
    void writeEntityRef(String name) throws XMLStreamException;

    /**
     * Writes the XML declaration with version 1.0 and no encoding.
     *
     * @throws XMLStreamException if it fails to write
     */
    void writeStartDocument() throws XMLStreamException;

    /**
     * Writes the XML declaration with a given version.
     *
     * @param version the version
     * @throws XMLStreamException if it fails to write
     */
    void writeStartDocument(String version) throws XMLStreamException;

    /**
     * Writes the XML declaration with encoding and version.
     *
     * <p>Watch out: the encoding name that is written <b>is only text</b>. It does not change how
     * the output is encoded, which was fixed when the writer was created. Writing {@code UTF-16} in
     * a writer that emits UTF-8 produces a document no tool can read, and the writer does not warn.
     *
     * @param encoding the name of the encoding to declare
     * @param version the version
     * @throws XMLStreamException if it fails to write
     */
    void writeStartDocument(String encoding, String version) throws XMLStreamException;

    /**
     * Writes text, escaping what is needed.
     *
     * @param text the text
     * @throws XMLStreamException if it fails to write
     */
    void writeCharacters(String text) throws XMLStreamException;

    /**
     * Writes text from an array, escaping what is needed.
     *
     * @param text the array
     * @param start from where
     * @param len how many characters
     * @throws XMLStreamException if it fails to write
     */
    void writeCharacters(char[] text, int start, int len) throws XMLStreamException;

    /**
     * The prefix bound to a namespace in the current context, or null.
     *
     * @param uri the namespace
     * @return the prefix
     * @throws XMLStreamException if it fails
     */
    String getPrefix(String uri) throws XMLStreamException;

    /**
     * Binds a prefix to a namespace for whatever is written from here on.
     *
     * <p>It does not write the declaration; see the class header.
     *
     * @param prefix the prefix
     * @param uri the namespace
     * @throws XMLStreamException if it fails
     */
    void setPrefix(String prefix, String uri) throws XMLStreamException;

    /**
     * Binds the default namespace, without writing the declaration.
     *
     * @param uri the namespace
     * @throws XMLStreamException if it fails
     */
    void setDefaultNamespace(String uri) throws XMLStreamException;

    /**
     * Replaces the whole namespace context.
     *
     * <p>It can only be called before the root element: changing the bindings halfway would
     * invalidate the prefixes already written.
     *
     * @param context the new context
     * @throws XMLStreamException if it fails or if it is too late
     */
    void setNamespaceContext(NamespaceContext context) throws XMLStreamException;

    /**
     * The bindings in force.
     *
     * @return the context; it cannot be modified by the caller
     */
    NamespaceContext getNamespaceContext();

    /**
     * The value of an implementation property.
     *
     * @param name the name of the property; cannot be null
     * @return the value
     * @throws IllegalArgumentException if the property does not exist
     */
    Object getProperty(String name) throws IllegalArgumentException;
}
