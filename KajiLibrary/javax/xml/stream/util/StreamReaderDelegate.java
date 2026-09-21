package javax.xml.stream.util;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * KajiLibrary's javax.xml.stream.util.StreamReaderDelegate -- a cursor reader that forwards
 * everything to another.
 *
 * <h2>What a class that does nothing is for</h2>
 *
 * <p>{@link XMLStreamReader} has forty-odd methods. Whoever wants to intercept <b>one</b> --count
 * elements, normalize text as it goes by, skip a branch-- would have to write the other forty by
 * hand if they implemented the interface directly. This class writes them once; the subclass
 * overrides the one it cares about and inherits the rest.
 *
 * <p>It is the decorator pattern with the boring part already done, and the same one
 * {@link java.io.FilterInputStream} uses for streams.
 *
 * <h2>The no-argument constructor and {@link #setParent}</h2>
 *
 * <p>It can be built without a reader and have it set later. It serves when the subclass needs to
 * compute something before knowing whom it wraps, or when the same decorator is reused over
 * successive readers. The price is that between construction and {@code setParent} any call blows
 * up with {@link NullPointerException}, which is the right failure: using a decorator with nothing
 * underneath is a caller error, not a state worth representing.
 *
 * <p>The methods are not synchronized and the parent field is not {@code volatile}: changing the
 * parent while another thread reads is not an intended use, here nor in the original.
 */
public class StreamReaderDelegate implements XMLStreamReader {

    /** Whom everything is forwarded to. */
    private XMLStreamReader reader;

    /**
     * A decorator without a reader yet.
     *
     * <p>{@link #setParent} has to be called before using it; see the header.
     */
    public StreamReaderDelegate() {
    }

    /**
     * A decorator over the given reader.
     *
     * @param reader the underlying reader
     */
    public StreamReaderDelegate(XMLStreamReader reader) {
        this.reader = reader;
    }

    /**
     * Changes the underlying reader.
     *
     * @param reader the new reader
     */
    public void setParent(XMLStreamReader reader) {
        this.reader = reader;
    }

    /**
     * The underlying reader.
     *
     * @return the reader, or null if it has not been set yet
     */
    public XMLStreamReader getParent() {
        return reader;
    }

    // ---- everything else is forwarding ----------------------------------------------------------

    /** {@inheritDoc} */
    public int next() throws XMLStreamException {
        return reader.next();
    }

    /** {@inheritDoc} */
    public int nextTag() throws XMLStreamException {
        return reader.nextTag();
    }

    /** {@inheritDoc} */
    public String getElementText() throws XMLStreamException {
        return reader.getElementText();
    }

    /** {@inheritDoc} */
    public void require(int type, String namespaceURI, String localName)
            throws XMLStreamException {
        reader.require(type, namespaceURI, localName);
    }

    /** {@inheritDoc} */
    public boolean hasNext() throws XMLStreamException {
        return reader.hasNext();
    }

    /** {@inheritDoc} */
    public void close() throws XMLStreamException {
        reader.close();
    }

    /** {@inheritDoc} */
    public String getNamespaceURI(String prefix) {
        return reader.getNamespaceURI(prefix);
    }

    /** {@inheritDoc} */
    public NamespaceContext getNamespaceContext() {
        return reader.getNamespaceContext();
    }

    /** {@inheritDoc} */
    public boolean isStartElement() {
        return reader.isStartElement();
    }

    /** {@inheritDoc} */
    public boolean isEndElement() {
        return reader.isEndElement();
    }

    /** {@inheritDoc} */
    public boolean isCharacters() {
        return reader.isCharacters();
    }

    /** {@inheritDoc} */
    public boolean isWhiteSpace() {
        return reader.isWhiteSpace();
    }

    /** {@inheritDoc} */
    public String getAttributeValue(String namespaceUri, String localName) {
        return reader.getAttributeValue(namespaceUri, localName);
    }

    /** {@inheritDoc} */
    public int getAttributeCount() {
        return reader.getAttributeCount();
    }

    /** {@inheritDoc} */
    public QName getAttributeName(int index) {
        return reader.getAttributeName(index);
    }

    /** {@inheritDoc} */
    public String getAttributePrefix(int index) {
        return reader.getAttributePrefix(index);
    }

    /** {@inheritDoc} */
    public String getAttributeNamespace(int index) {
        return reader.getAttributeNamespace(index);
    }

    /** {@inheritDoc} */
    public String getAttributeLocalName(int index) {
        return reader.getAttributeLocalName(index);
    }

    /** {@inheritDoc} */
    public String getAttributeType(int index) {
        return reader.getAttributeType(index);
    }

    /** {@inheritDoc} */
    public String getAttributeValue(int index) {
        return reader.getAttributeValue(index);
    }

    /** {@inheritDoc} */
    public boolean isAttributeSpecified(int index) {
        return reader.isAttributeSpecified(index);
    }

    /** {@inheritDoc} */
    public int getNamespaceCount() {
        return reader.getNamespaceCount();
    }

    /** {@inheritDoc} */
    public String getNamespacePrefix(int index) {
        return reader.getNamespacePrefix(index);
    }

    /** {@inheritDoc} */
    public String getNamespaceURI(int index) {
        return reader.getNamespaceURI(index);
    }

    /** {@inheritDoc} */
    public int getEventType() {
        return reader.getEventType();
    }

    /** {@inheritDoc} */
    public String getText() {
        return reader.getText();
    }

    /** {@inheritDoc} */
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
            throws XMLStreamException {
        return reader.getTextCharacters(sourceStart, target, targetStart, length);
    }

    /** {@inheritDoc} */
    public char[] getTextCharacters() {
        return reader.getTextCharacters();
    }

    /** {@inheritDoc} */
    public int getTextStart() {
        return reader.getTextStart();
    }

    /** {@inheritDoc} */
    public int getTextLength() {
        return reader.getTextLength();
    }

    /** {@inheritDoc} */
    public String getEncoding() {
        return reader.getEncoding();
    }

    /** {@inheritDoc} */
    public boolean hasText() {
        return reader.hasText();
    }

    /** {@inheritDoc} */
    public Location getLocation() {
        return reader.getLocation();
    }

    /** {@inheritDoc} */
    public QName getName() {
        return reader.getName();
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return reader.getLocalName();
    }

    /** {@inheritDoc} */
    public boolean hasName() {
        return reader.hasName();
    }

    /** {@inheritDoc} */
    public String getNamespaceURI() {
        return reader.getNamespaceURI();
    }

    /** {@inheritDoc} */
    public String getPrefix() {
        return reader.getPrefix();
    }

    /** {@inheritDoc} */
    public String getVersion() {
        return reader.getVersion();
    }

    /** {@inheritDoc} */
    public boolean isStandalone() {
        return reader.isStandalone();
    }

    /** {@inheritDoc} */
    public boolean standaloneSet() {
        return reader.standaloneSet();
    }

    /** {@inheritDoc} */
    public String getCharacterEncodingScheme() {
        return reader.getCharacterEncodingScheme();
    }

    /** {@inheritDoc} */
    public String getPITarget() {
        return reader.getPITarget();
    }

    /** {@inheritDoc} */
    public String getPIData() {
        return reader.getPIData();
    }

    /** {@inheritDoc} */
    public Object getProperty(String name) {
        return reader.getProperty(name);
    }
}
