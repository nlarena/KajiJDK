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
 * La fabrica de lectura de esta biblioteca.
 *
 * <h2>Las propiedades que se pueden cambiar y las que no</h2>
 *
 * <p>{@link #setProperty} acepta las tres que el parser sabe respetar y rechaza las otras tres con
 * {@link IllegalArgumentException}, salvo que el valor que se pida sea el que ya tienen. Aceptar en
 * silencio un {@code setProperty(IS_VALIDATING, TRUE)} y despues no validar es la clase de mentira
 * que esta biblioteca no comete: el llamador se entera aca y no cuando un documento invalido pasa
 * como bueno. Ver {@link XMLInputFactory}.
 *
 * <h2>La codificacion de un flujo de bytes</h2>
 *
 * <p>Un {@link InputStream} son bytes y XML es texto, asi que alguien tiene que decidir con que
 * codificacion leerlos. El orden es el que manda la especificacion: la que pida el llamador gana;
 * si no pide, la marca de orden de bytes; si no hay, la que declare el propio documento --que se
 * puede leer como ASCII porque la declaracion esta obligada a ser ASCII--; y si tampoco, UTF-8.
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

    // ---- lectores de cursor -----------------------------------------------------------------

    public XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
        return createXMLStreamReader(null, reader);
    }

    public XMLStreamReader createXMLStreamReader(String systemId, Reader reader)
            throws XMLStreamException {
        if (reader == null) {
            throw new XMLStreamException("el lector no puede ser null");
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

    // ---- lectores de eventos ----------------------------------------------------------------

    public XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(reader));
    }

    public XMLEventReader createXMLEventReader(String systemId, Reader reader)
            throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(systemId, reader));
    }

    public XMLEventReader createXMLEventReader(XMLStreamReader reader) throws XMLStreamException {
        if (reader == null) {
            throw new XMLStreamException("el lector no puede ser null");
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

    // ---- filtros ----------------------------------------------------------------------------

    public XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter)
            throws XMLStreamException {
        if (reader == null || filter == null) {
            throw new XMLStreamException("ni el lector ni el filtro pueden ser null");
        }
        return new KajiFilteredStreamReader(reader, filter);
    }

    public XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter)
            throws XMLStreamException {
        if (reader == null || filter == null) {
            throw new XMLStreamException("ni el lector ni el filtro pueden ser null");
        }
        return new KajiFilteredEventReader(reader, filter);
    }

    // ---- configuracion ----------------------------------------------------------------------

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
            throw new IllegalArgumentException("el asignador no puede ser null");
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
            throw new IllegalArgumentException("el nombre de la propiedad no puede ser null");
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
            throw new IllegalArgumentException("el nombre de la propiedad no puede ser null");
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
                        name + " no se puede encender: este parser no lee el DTD ni valida");
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
        throw new IllegalArgumentException(name + " toma un booleano, y le dieron " + value);
    }

    // ---- entradas ---------------------------------------------------------------------------

    private XMLStreamReader fromSource(Source source) throws XMLStreamException {
        if (source == null) {
            throw new XMLStreamException("la fuente no puede ser null");
        }
        if (!(source instanceof StreamSource)) {
            throw new XMLStreamException(
                    "esta biblioteca solo lee de un StreamSource, y le dieron un "
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
            throw new XMLStreamException("el StreamSource esta vacio");
        }
        InputStream in;
        try {
            in = new FileInputStream(new File(fileOf(sid)));
        } catch (IOException e) {
            throw new XMLStreamException("no se pudo abrir " + sid, e);
        }
        return fromBytes(sid, in, null);
    }

    /** Un {@code file:} se abre como archivo; cualquier otro esquema no se sabe resolver. */
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
        int dosPuntos = systemId.indexOf(':');
        if (dosPuntos > 1) {
            throw new XMLStreamException(
                    "esta biblioteca solo sabe abrir identificadores de sistema locales, y le "
                            + "dieron " + systemId);
        }
        return systemId;
    }

    private XMLStreamReader fromBytes(String systemId, InputStream in, String encoding)
            throws XMLStreamException {
        if (in == null) {
            throw new XMLStreamException("el flujo no puede ser null");
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
            throw new XMLStreamException("no se conoce la codificacion " + enc, e);
        }
        return new KajiStreamReader(new StringReader(text), systemId, enc, coalescing,
                namespaceAware, replacingEntityRefs);
    }

    /**
     * El {@code encoding="..."} de la declaracion XML, leido como ASCII.
     *
     * <p>Se puede hacer asi porque la especificacion obliga a que la declaracion sea representable
     * en ASCII sea cual sea la codificacion del resto: si no, no habria forma de arrancar.
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
            throw new XMLStreamException("no se pudo leer la entrada", e);
        }
        return out.toByteArray();
    }
}
