package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * This library's XML writer, in cursor form.
 *
 * <h2>The open tag</h2>
 *
 * <p>All the machinery revolves around one state: {@code <a} written and the {@code >} not yet,
 * because an attribute may come. Anything that is not an attribute or a declaration closes the tag
 * first. It is what allows the API to have {@code writeStartElement} and {@code writeAttribute} as
 * separate calls without the caller having to say when they finished putting attributes.
 *
 * <h2>The two namespace modes</h2>
 *
 * <p>With {@link XMLOutputFactory#IS_REPAIRING_NAMESPACES} off the writer writes what it is told:
 * if a prefix nobody declared is used, a malformed document comes out, and it is the caller's
 * responsibility. On, before writing a qualified name it checks whether its namespace is in scope
 * and, if not, emits the declaration --inventing a prefix if needed--.
 *
 * <p>What repairing mode does not do is guess intentions: if a prefix is asked for explicitly, that
 * one is used; what is added is the declaration that was missing.
 *
 * <h2>What it does not check</h2>
 *
 * <p>It does not verify that the document has a single root element, nor that names are valid XML
 * names, nor that a comment's text does not contain {@code --}. A writer that validates all that is
 * useful, but it costs on the hot path and the specification does not ask for it; what is checked
 * is the structural --closing an element that is not open, writing an attribute outside a tag--
 * because that produces silent garbage instead of an error.
 */
final class KajiStreamWriter implements XMLStreamWriter {

    private final Writer w;
    private final boolean repairing;

    private final KajiNsContext ctx = new KajiNsContext();
    private NamespaceContext rootCtx;

    /** Prefixes declared with {@code setPrefix} that do not yet have an element to live in. */
    private String[] pendingPrefix = new String[4];
    private String[] pendingUri = new String[4];
    private int pendingCount;

    private String[] stack = new String[16];
    private int depth;

    private boolean tagOpen;
    private boolean tagEmpty;
    private int invented;

    KajiStreamWriter(Writer w, boolean repairing) {
        this.w = w;
        this.repairing = repairing;
    }

    // ---- plumbing ---------------------------------------------------------------------------

    private void emit(String s) throws XMLStreamException {
        try {
            w.write(s);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    private void closeTag() throws XMLStreamException {
        if (!tagOpen) {
            return;
        }
        tagOpen = false;
        if (tagEmpty) {
            tagEmpty = false;
            emit("/>");
            ctx.closeScope();
        } else {
            emit(">");
        }
    }

    private void requireOpenTag() throws XMLStreamException {
        if (!tagOpen) {
            throw new XMLStreamException(
                    "this can only be written inside a start tag");
        }
    }

    private void flushPending() {
        for (int i = 0; i < pendingCount; i++) {
            ctx.declare(pendingPrefix[i], pendingUri[i]);
        }
        pendingCount = 0;
    }

    private void addPending(String prefix, String uri) {
        if (pendingCount == pendingPrefix.length) {
            String[] p = new String[pendingCount * 2];
            String[] u = new String[pendingCount * 2];
            System.arraycopy(pendingPrefix, 0, p, 0, pendingCount);
            System.arraycopy(pendingUri, 0, u, 0, pendingCount);
            pendingPrefix = p;
            pendingUri = u;
        }
        pendingPrefix[pendingCount] = prefix;
        pendingUri[pendingCount] = uri;
        pendingCount++;
    }

    /**
     * The prefix in scope for a URI, looking first at its own and then at the context that was set.
     */
    private String prefixFor(String uri) {
        String p = ctx.getPrefix(uri);
        if (p != null) {
            return p;
        }
        for (int i = 0; i < pendingCount; i++) {
            if (pendingUri[i].equals(uri)) {
                return pendingPrefix[i];
            }
        }
        if (rootCtx != null) {
            return rootCtx.getPrefix(uri);
        }
        return null;
    }

    private String uriFor(String prefix) {
        String u = ctx.getNamespaceURI(prefix);
        if (u != null && u.length() > 0) {
            return u;
        }
        for (int i = 0; i < pendingCount; i++) {
            if (pendingPrefix[i].equals(prefix)) {
                return pendingUri[i];
            }
        }
        if (rootCtx != null) {
            return rootCtx.getNamespaceURI(prefix);
        }
        return null;
    }

    private void push(String written) {
        if (depth == stack.length) {
            String[] bigger = new String[stack.length * 2];
            System.arraycopy(stack, 0, bigger, 0, stack.length);
            stack = bigger;
        }
        stack[depth] = written;
        depth++;
    }

    // ---- elements ---------------------------------------------------------------------------

    public void writeStartElement(String localName) throws XMLStreamException {
        closeTag();
        ctx.openScope();
        flushPending();
        emit("<" + localName);
        push(localName);
        tagOpen = true;
    }

    public void writeStartElement(String namespaceURI, String localName)
            throws XMLStreamException {
        startTag(null, namespaceURI, localName, false);
    }

    public void writeStartElement(String prefix, String localName, String namespaceURI)
            throws XMLStreamException {
        startTag(prefix, namespaceURI, localName, false);
    }

    public void writeEmptyElement(String localName) throws XMLStreamException {
        closeTag();
        ctx.openScope();
        flushPending();
        emit("<" + localName);
        tagOpen = true;
        tagEmpty = true;
    }

    public void writeEmptyElement(String namespaceURI, String localName)
            throws XMLStreamException {
        startTag(null, namespaceURI, localName, true);
    }

    public void writeEmptyElement(String prefix, String localName, String namespaceURI)
            throws XMLStreamException {
        startTag(prefix, namespaceURI, localName, true);
    }

    private void startTag(String prefix, String uri, String local, boolean empty)
            throws XMLStreamException {
        closeTag();
        ctx.openScope();
        flushPending();
        String p = prefix;
        boolean mustDeclare = false;
        if (uri == null || uri.length() == 0) {
            p = XMLConstants.DEFAULT_NS_PREFIX;
        } else if (p == null) {
            p = prefixFor(uri);
            if (p == null) {
                if (!repairing) {
                    throw new XMLStreamException(
                            "the namespace " + uri + " has no prefix in scope; "
                                    + "declare it first, or turn on isRepairingNamespaces");
                }
                p = XMLConstants.DEFAULT_NS_PREFIX;
                mustDeclare = true;
            }
        } else {
            String bound = uriFor(p);
            if (bound == null || !bound.equals(uri)) {
                if (repairing) {
                    mustDeclare = true;
                } else {
                    // Without repairing the caller writes the declaration; the binding is noted
                    // anyway so that getPrefix() tells the truth.
                    ctx.declare(p, uri);
                }
            }
        }
        String written;
        if (p == null || p.length() == 0) {
            written = local;
        } else {
            written = p + ":" + local;
        }
        emit("<" + written);
        if (mustDeclare) {
            ctx.declare(p, uri);
            if (p.length() == 0) {
                emit(" " + XMLConstants.XMLNS_ATTRIBUTE + "=\"");
            } else {
                emit(" " + XMLConstants.XMLNS_ATTRIBUTE + ":" + p + "=\"");
            }
            emitEscapedValue(uri);
            emit("\"");
        }
        tagOpen = true;
        if (empty) {
            tagEmpty = true;
        } else {
            push(written);
        }
    }

    /**
     * Closes the most recent open element.
     *
     * <p>If the last thing written was a {@link #writeEmptyElement}, this call closes the element
     * THAT CONTAINS IT, not the empty one -- the empty one already closes itself with its `/>`. It
     * is what the JDK does: `writeStartElement("r"); writeEmptyElement("e"); writeEndElement();`
     * produces `<r><e/></r>`.
     *
     * <p>This used to throw an exception until the behaviour test was run against JDK 25 and did
     * not match. It seemed reasonable --nobody "closes" an empty element-- but it misreads the
     * call: `writeEndElement` does not say which one it closes, it closes whichever is open, and
     * after an empty one the open one is the outer one.
     */
    public void writeEndElement() throws XMLStreamException {
        closeTag();
        if (depth == 0) {
            throw new XMLStreamException("there is no open element");
        }
        depth--;
        emit("</" + stack[depth] + ">");
        ctx.closeScope();
    }

    public void writeEndDocument() throws XMLStreamException {
        closeTag();
        while (depth > 0) {
            writeEndElement();
        }
    }

    // ---- attributes and declarations ------------------------------------------------------------

    private void emitEscapedValue(String v) throws XMLStreamException {
        try {
            Escapes.attribute(w, v);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    public void writeAttribute(String localName, String value) throws XMLStreamException {
        requireOpenTag();
        emit(" " + localName + "=\"");
        emitEscapedValue(value);
        emit("\"");
    }

    public void writeAttribute(String namespaceURI, String localName, String value)
            throws XMLStreamException {
        attribute(null, namespaceURI, localName, value);
    }

    public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
            throws XMLStreamException {
        attribute(prefix, namespaceURI, localName, value);
    }

    private void attribute(String prefix, String uri, String local, String value)
            throws XMLStreamException {
        requireOpenTag();
        if (uri == null || uri.length() == 0) {
            writeAttribute(local, value);
            return;
        }
        String p = prefix;
        if (p == null || p.length() == 0) {
            p = prefixFor(uri);
        }
        boolean declare = false;
        if (p == null || p.length() == 0) {
            // An attribute without a prefix does not fall into the default namespace, so here a
            // real one is needed; in repairing mode one is invented.
            if (!repairing) {
                throw new XMLStreamException(
                        "the namespace " + uri + " has no prefix in scope for an "
                                + "attribute; declare it first, or turn on isRepairingNamespaces");
            }
            invented++;
            p = "ns" + invented;
            declare = true;
        } else {
            String bound = uriFor(p);
            if (bound == null || !bound.equals(uri)) {
                if (repairing) {
                    declare = true;
                } else {
                    ctx.declare(p, uri);
                }
            }
        }
        if (declare) {
            ctx.declare(p, uri);
            emit(" " + XMLConstants.XMLNS_ATTRIBUTE + ":" + p + "=\"");
            emitEscapedValue(uri);
            emit("\"");
        }
        emit(" " + p + ":" + local + "=\"");
        emitEscapedValue(value);
        emit("\"");
    }

    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        requireOpenTag();
        String p = prefix;
        if (p == null || p.length() == 0 || p.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            writeDefaultNamespace(namespaceURI);
            return;
        }
        String bound = ctx.getNamespaceURI(p);
        ctx.declare(p, namespaceURI);
        if (repairing && bound != null && bound.equals(namespaceURI)) {
            return;
        }
        emit(" " + XMLConstants.XMLNS_ATTRIBUTE + ":" + p + "=\"");
        emitEscapedValue(namespaceURI);
        emit("\"");
    }

    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        requireOpenTag();
        ctx.declare(XMLConstants.DEFAULT_NS_PREFIX, namespaceURI);
        emit(" " + XMLConstants.XMLNS_ATTRIBUTE + "=\"");
        emitEscapedValue(namespaceURI);
        emit("\"");
    }

    // ---- content ----------------------------------------------------------------------------

    public void writeCharacters(String text) throws XMLStreamException {
        closeTag();
        try {
            Escapes.content(w, text);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        closeTag();
        try {
            Escapes.content(w, text, start, len);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    public void writeCData(String data) throws XMLStreamException {
        closeTag();
        emit("<![CDATA[" + data + "]]>");
    }

    public void writeComment(String data) throws XMLStreamException {
        closeTag();
        if (data == null) {
            emit("<!---->");
        } else {
            emit("<!--" + data + "-->");
        }
    }

    public void writeEntityRef(String name) throws XMLStreamException {
        closeTag();
        emit("&" + name + ";");
    }

    public void writeDTD(String dtd) throws XMLStreamException {
        closeTag();
        emit(dtd);
    }

    public void writeProcessingInstruction(String target) throws XMLStreamException {
        closeTag();
        emit("<?" + target + "?>");
    }

    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        closeTag();
        if (data == null || data.length() == 0) {
            emit("<?" + target + "?>");
        } else {
            emit("<?" + target + " " + data + "?>");
        }
    }

    public void writeStartDocument() throws XMLStreamException {
        emit("<?xml version=\"1.0\" ?>");
    }

    public void writeStartDocument(String version) throws XMLStreamException {
        if (version == null) {
            writeStartDocument();
            return;
        }
        emit("<?xml version=\"" + version + "\" ?>");
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        String v = version;
        if (v == null) {
            v = "1.0";
        }
        if (encoding == null) {
            emit("<?xml version=\"" + v + "\" ?>");
        } else {
            emit("<?xml version=\"" + v + "\" encoding=\"" + encoding + "\"?>");
        }
    }

    // ---- state ------------------------------------------------------------------------------

    public String getPrefix(String uri) throws XMLStreamException {
        return prefixFor(uri);
    }

    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        if (prefix == null) {
            throw new XMLStreamException("the prefix cannot be null");
        }
        if (uri == null) {
            throw new XMLStreamException("the namespace cannot be null");
        }
        addPending(prefix, uri);
    }

    public void setDefaultNamespace(String uri) throws XMLStreamException {
        if (uri == null) {
            throw new XMLStreamException("the namespace cannot be null");
        }
        addPending(XMLConstants.DEFAULT_NS_PREFIX, uri);
    }

    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        if (depth > 0 || tagOpen) {
            throw new XMLStreamException(
                    "the namespace context is set before writing the root");
        }
        rootCtx = context;
    }

    public NamespaceContext getNamespaceContext() {
        return ctx.snapshot();
    }

    public Object getProperty(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("the property name cannot be null");
        }
        if (name.equals(XMLOutputFactory.IS_REPAIRING_NAMESPACES)) {
            return Boolean.valueOf(repairing);
        }
        throw new IllegalArgumentException("propiedad desconocida: " + name);
    }

    public void flush() throws XMLStreamException {
        try {
            w.flush();
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    public void close() throws XMLStreamException {
        closeTag();
        flush();
    }
}
