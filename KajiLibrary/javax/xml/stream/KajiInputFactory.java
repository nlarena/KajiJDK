package javax.xml.stream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * This library's reading factory.
 *
 * <h2>The properties that can be changed and those that cannot</h2>
 *
 * <p>{@link #setProperty} accepts the three the parser knows how to honour and rejects the other
 * three with {@link IllegalArgumentException}, unless the value asked for is the one they already
 * have. Silently accepting a {@code setProperty(IS_VALIDATING, TRUE)} and then not validating is
 * the kind of lie this library does not tell: the caller finds out here and not when an invalid
 * document passes as good. See {@link XMLInputFactory}.
 *
 * <h2>The encoding of a byte stream</h2>
 *
 * <p>An {@link InputStream} is bytes and XML is text, so somebody has to decide which encoding to
 * read them with. The order is the one the specification dictates: the one the caller asks for
 * wins; if it does not ask, the byte order mark; if there is none, the one the document itself
 * declares --which can be read as ASCII because the declaration is required to be ASCII--; and if
 * not that either, UTF-8.
 */
final class KajiInputFactory extends XMLInputFactory {

    private boolean coalescing;
    private boolean namespaceAware = true;
    private boolean replacingEntityRefs = true;
    private XMLReporter reporter;
    private XMLResolver resolver;
    private XMLEventAllocator allocator = new KajiAllocator();

    KajiInputFactory() {
    }

    // ---- cursor readers -------------------------------------------------------------------------

    public XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
        return createXMLStreamReader(null, reader);
    }

    public XMLStreamReader createXMLStreamReader(String systemId, Reader reader)
            throws XMLStreamException {
        if (reader == null) {
            throw new XMLStreamException("the reader cannot be null");
        }
        return new KajiStreamReader(reader, systemId, null, coalescing, namespaceAware,
                replacingEntityRefs);
    }

    public XMLStreamReader createXMLStreamReader(InputStream stream) throws XMLStreamException {
        return fromBytes(null, stream, null);
    }

    public XMLStreamReader createXMLStreamReader(InputStream stream, String encoding)
            throws XMLStreamException {
        return fromBytes(null, stream, encoding);
    }

    public XMLStreamReader createXMLStreamReader(String systemId, InputStream stream)
            throws XMLStreamException {
        return fromBytes(systemId, stream, null);
    }

    public XMLStreamReader createXMLStreamReader(Source source) throws XMLStreamException {
        return fromSource(source);
    }

    // ---- event readers --------------------------------------------------------------------------

    public XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(reader));
    }

    public XMLEventReader createXMLEventReader(String systemId, Reader reader)
            throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(systemId, reader));
    }

    public XMLEventReader createXMLEventReader(XMLStreamReader reader) throws XMLStreamException {
        if (reader == null) {
            throw new XMLStreamException("the reader cannot be null");
        }
        return new KajiEventReader(reader, allocator.newInstance());
    }

    public XMLEventReader createXMLEventReader(Source source) throws XMLStreamException {
        return createXMLEventReader(fromSource(source));
    }

    public XMLEventReader createXMLEventReader(InputStream stream) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(stream));
    }

    public XMLEventReader createXMLEventReader(InputStream stream, String encoding)
            throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(stream, encoding));
    }

    public XMLEventReader createXMLEventReader(String systemId, InputStream stream)
            throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(systemId, stream));
    }

    // ---- filters ----------------------------------------------------------------------------

    public XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter)
            throws XMLStreamException {
        if (reader == null || filter == null) {
            throw new XMLStreamException("neither the reader nor the filter can be null");
        }
        return new KajiFilteredStreamReader(reader, filter);
    }

    public XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter)
            throws XMLStreamException {
        if (reader == null || filter == null) {
            throw new XMLStreamException("neither the reader nor the filter can be null");
        }
        return new KajiFilteredEventReader(reader, filter);
    }

    // ---- configuration ----------------------------------------------------------------------

    public XMLResolver getXMLResolver() {
        return resolver;
    }

    public void setXMLResolver(XMLResolver resolver) {
        this.resolver = resolver;
    }

    public XMLReporter getXMLReporter() {
        return reporter;
    }

    public void setXMLReporter(XMLReporter reporter) {
        this.reporter = reporter;
    }

    public void setEventAllocator(XMLEventAllocator allocator) {
        if (allocator == null) {
            throw new IllegalArgumentException("the allocator cannot be null");
        }
        this.allocator = allocator;
    }

    public XMLEventAllocator getEventAllocator() {
        return allocator;
    }

    public boolean isPropertySupported(String name) {
        if (name == null) {
            return false;
        }
        return name.equals(IS_COALESCING) || name.equals(IS_NAMESPACE_AWARE)
                || name.equals(IS_REPLACING_ENTITY_REFERENCES) || name.equals(IS_VALIDATING)
                || name.equals(IS_SUPPORTING_EXTERNAL_ENTITIES) || name.equals(SUPPORT_DTD)
                || name.equals(REPORTER) || name.equals(RESOLVER) || name.equals(ALLOCATOR);
    }

    public Object getProperty(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("the property name cannot be null");
        }
        if (name.equals(IS_COALESCING)) {
            return Boolean.valueOf(coalescing);
        }
        if (name.equals(IS_NAMESPACE_AWARE)) {
            return Boolean.valueOf(namespaceAware);
        }
        if (name.equals(IS_REPLACING_ENTITY_REFERENCES)) {
            return Boolean.valueOf(replacingEntityRefs);
        }
        if (name.equals(IS_VALIDATING) || name.equals(IS_SUPPORTING_EXTERNAL_ENTITIES)
                || name.equals(SUPPORT_DTD)) {
            return Boolean.FALSE;
        }
        if (name.equals(REPORTER)) {
            return reporter;
        }
        if (name.equals(RESOLVER)) {
            return resolver;
        }
        if (name.equals(ALLOCATOR)) {
            return allocator;
        }
        throw new IllegalArgumentException("propiedad desconocida: " + name);
    }

    public void setProperty(String name, Object value) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("the property name cannot be null");
        }
        if (name.equals(IS_COALESCING)) {
            coalescing = asBoolean(name, value);
            return;
        }
        if (name.equals(IS_NAMESPACE_AWARE)) {
            namespaceAware = asBoolean(name, value);
            return;
        }
        if (name.equals(IS_REPLACING_ENTITY_REFERENCES)) {
            replacingEntityRefs = asBoolean(name, value);
            return;
        }
        if (name.equals(IS_VALIDATING) || name.equals(IS_SUPPORTING_EXTERNAL_ENTITIES)
                || name.equals(SUPPORT_DTD)) {
            if (asBoolean(name, value)) {
                throw new IllegalArgumentException(
                        name + " cannot be turned on: this parser skips DTDs and validation");
            }
            return;
        }
        if (name.equals(REPORTER)) {
            reporter = (XMLReporter) value;
            return;
        }
        if (name.equals(RESOLVER)) {
            resolver = (XMLResolver) value;
            return;
        }
        if (name.equals(ALLOCATOR)) {
            setEventAllocator((XMLEventAllocator) value);
            return;
        }
        throw new IllegalArgumentException("propiedad desconocida: " + name);
    }

    private static boolean asBoolean(String name, Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        if (value instanceof String) {
            return Boolean.valueOf((String) value).booleanValue();
        }
        throw new IllegalArgumentException(name + " takes a boolean, and was given " + value);
    }

    // ---- inputs -----------------------------------------------------------------------------

    private XMLStreamReader fromSource(Source source) throws XMLStreamException {
        if (source == null) {
            throw new XMLStreamException("the source cannot be null");
        }
        if (!(source instanceof StreamSource)) {
            throw new XMLStreamException(
                    "this library only reads from a StreamSource, and was given a "
                            + source.getClass().getName());
        }
        StreamSource s = (StreamSource) source;
        if (s.getReader() != null) {
            return createXMLStreamReader(s.getSystemId(), s.getReader());
        }
        if (s.getInputStream() != null) {
            return fromBytes(s.getSystemId(), s.getInputStream(), null);
        }
        String sid = s.getSystemId();
        if (sid == null) {
            throw new XMLStreamException("the StreamSource is empty");
        }
        InputStream in;
        try {
            in = new FileInputStream(new File(fileOf(sid)));
        } catch (IOException e) {
            throw new XMLStreamException("could not open " + sid, e);
        }
        return fromBytes(sid, in, null);
    }

    /** A {@code file:} is opened as a file; any other scheme cannot be resolved. */
    private static String fileOf(String systemId) throws XMLStreamException {
        if (systemId.startsWith("file:///")) {
            return systemId.substring(8);
        }
        if (systemId.startsWith("file://")) {
            return systemId.substring(7);
        }
        if (systemId.startsWith("file:")) {
            return systemId.substring(5);
        }
        int colon = systemId.indexOf(':');
        if (colon > 1) {
            throw new XMLStreamException(
                    "this library can only open local system identifiers, and was "
                            + "dieron " + systemId);
        }
        return systemId;
    }

    private XMLStreamReader fromBytes(String systemId, InputStream in, String encoding)
            throws XMLStreamException {
        if (in == null) {
            throw new XMLStreamException("the stream cannot be null");
        }
        byte[] b = readAll(in);
        int from = 0;
        String enc = encoding;
        if (enc == null) {
            if (b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB
                    && (b[2] & 0xFF) == 0xBF) {
                enc = "UTF-8";
                from = 3;
            } else if (b.length >= 2 && (b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) {
                enc = "UTF-16BE";
                from = 2;
            } else if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) {
                enc = "UTF-16LE";
                from = 2;
            } else {
                enc = declaredEncodingOf(b);
            }
        }
        if (enc == null) {
            enc = "UTF-8";
        }
        String text;
        try {
            text = new String(b, from, b.length - from, enc);
        } catch (UnsupportedEncodingException e) {
            throw new XMLStreamException("unknown encoding " + enc, e);
        }
        return new KajiStreamReader(new StringReader(text), systemId, enc, coalescing,
                namespaceAware, replacingEntityRefs);
    }

    /**
     * The {@code encoding="..."} of the XML declaration, read as ASCII.
     *
     * <p>It can be done this way because the specification requires the declaration to be
     * representable in ASCII whatever the encoding of the rest: otherwise there would be no way to
     * start.
     */
    private static String declaredEncodingOf(byte[] b) {
        int n = b.length;
        if (n > 200) {
            n = 200;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append((char) (b[i] & 0xFF));
        }
        String s = sb.toString();
        if (!s.startsWith("<?xml")) {
            return null;
        }
        int end = s.indexOf("?>");
        if (end < 0) {
            return null;
        }
        int i = s.indexOf("encoding", 0);
        if (i < 0 || i > end) {
            return null;
        }
        int eq = s.indexOf('=', i);
        if (eq < 0 || eq > end) {
            return null;
        }
        int j = eq + 1;
        while (j < end && (s.charAt(j) == ' ' || s.charAt(j) == '\t' || s.charAt(j) == '\n'
                || s.charAt(j) == '\r')) {
            j++;
        }
        if (j >= end) {
            return null;
        }
        char quote = s.charAt(j);
        if (quote != '"' && quote != '\'') {
            return null;
        }
        int endQuote = s.indexOf(quote, j + 1);
        if (endQuote < 0 || endQuote > end) {
            return null;
        }
        return s.substring(j + 1, endQuote);
    }

    private static byte[] readAll(InputStream in) throws XMLStreamException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        try {
            while (true) {
                int n = in.read(b, 0, b.length);
                if (n < 0) {
                    break;
                }
                out.write(b, 0, n);
            }
        } catch (IOException e) {
            throw new XMLStreamException("could not read the input", e);
        }
        return out.toByteArray();
    }
}
