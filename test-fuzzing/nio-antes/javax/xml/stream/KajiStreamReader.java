package javax.xml.stream;

import java.io.IOException;
import java.io.Reader;
import java.util.NoSuchElementException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 * This library's XML 1.0 parser, in cursor form.
 *
 * <h2>What it recognizes</h2>
 *
 * <p>Well-formed documents with namespaces: XML declaration, processing instructions, comments,
 * {@code <!DOCTYPE>} (delivered raw, uninterpreted), elements, attributes, text, CDATA sections,
 * the five predefined entities and decimal and hexadecimal character references.
 *
 * <p>What it does not do is written in {@link XMLInputFactory}: it does not interpret the DTD, does
 * not resolve external entities and does not validate.
 *
 * <h2>It reads everything up front, and why</h2>
 *
 * <p>The constructor consumes the whole {@link Reader} before returning the first event. A truly
 * incremental parser has to handle tokens split between two buffer fills, which is where the
 * hardest bugs of a parser come from; here the cost is memory proportional to the document and in
 * exchange that kind of bug does not exist.
 *
 * <p>The visible consequence: closing the reader halfway saves no reading, because it was already
 * read. And a document that never ends --a socket-- cannot be processed a bit at a time. It is a
 * real limitation of this implementation, not of the API.
 *
 * <h2>Line endings are normalized once</h2>
 *
 * <p>XML requires converting {@code \r\n} and {@code \r} to {@code \n} before parsing. It is done
 * on the whole buffer when loading it, so the rest of the code --and the line count-- never has to
 * think about it again.
 */
final class KajiStreamReader implements XMLStreamReader {

    // ---- configuration ----------------------------------------------------------------------

    private final boolean coalescing;
    private final boolean namespaceAware;
    private final boolean replacingEntityRefs;
    private final String systemId;
    private final String sourceEncoding;

    // ---- the text -------------------------------------------------------------------------------

    private final char[] buf;
    private final int end;
    private int pos;
    private int line = 1;
    private int column = 1;

    // ---- the current event ----------------------------------------------------------------------

    private int eventType = XMLStreamConstants.START_DOCUMENT;
    private String text;
    private QName currentName;
    private String piTarget;
    private String piData;
    private String entityName;
    private KajiLocation location;

    // ---- the prolog -----------------------------------------------------------------------------

    private String version = "1.0";
    private String declaredEncoding;
    private boolean standalone;
    private boolean standaloneDeclared;

    // ---- attributes of the current element ------------------------------------------------------

    private String[] attrPrefix = new String[8];
    private String[] attrUri = new String[8];
    private String[] attrLocal = new String[8];
    private String[] attrValue = new String[8];
    private int attrCount;

    // ---- the structure --------------------------------------------------------------------------

    private final KajiNsContext context = new KajiNsContext();
    private QName[] stack = new QName[16];
    private int depth;
    private boolean emptyElement;
    private boolean popScopeOnNext;
    private boolean finished;
    private boolean closed;
    private boolean sawRoot;

    // ---- construction -----------------------------------------------------------------------

    KajiStreamReader(Reader in, String systemId, String sourceEncoding,
            boolean coalescing, boolean namespaceAware, boolean replacingEntityRefs)
            throws XMLStreamException {
        this.systemId = systemId;
        this.sourceEncoding = sourceEncoding;
        this.coalescing = coalescing;
        this.namespaceAware = namespaceAware;
        this.replacingEntityRefs = replacingEntityRefs;
        char[] all = loadAll(in);
        this.buf = all;
        this.end = all.length;
        this.location = here();
        readXmlDeclaration();
        this.location = here();
    }

    /** Reads the whole stream and normalizes line endings in a single pass. */
    private static char[] loadAll(Reader r) throws XMLStreamException {
        char[] b = new char[8192];
        int n = 0;
        try {
            while (true) {
                if (n == b.length) {
                    char[] bigger = new char[b.length * 2];
                    System.arraycopy(b, 0, bigger, 0, n);
                    b = bigger;
                }
                int nRead = r.read(b, n, b.length - n);
                if (nRead < 0) {
                    break;
                }
                n += nRead;
            }
        } catch (IOException e) {
            throw new XMLStreamException("could not read the input", e);
        }
        char[] clean = new char[n];
        int m = 0;
        for (int i = 0; i < n; i++) {
            char c = b[i];
            if (c == '\r') {
                clean[m] = '\n';
                m++;
                if (i + 1 < n && b[i + 1] == '\n') {
                    i++;
                }
            } else {
                clean[m] = c;
                m++;
            }
        }
        if (m == clean.length) {
            return clean;
        }
        char[] exact = new char[m];
        System.arraycopy(clean, 0, exact, 0, m);
        return exact;
    }

    // ---- reading characters ---------------------------------------------------------------------

    private KajiLocation here() {
        return new KajiLocation(line, column, pos, null, systemId);
    }

    private boolean hasAhead(int k) {
        return pos + k < end;
    }

    private char peekAt(int k) {
        return buf[pos + k];
    }

    private char nextChar() throws XMLStreamException {
        if (pos >= end) {
            throw error("the document ends prematurely");
        }
        char c = buf[pos];
        pos++;
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private boolean lookingAt(String s) {
        int n = s.length();
        if (pos + n > end) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (buf[pos + i] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private void skipOver(String s) throws XMLStreamException {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            nextChar();
        }
    }

    private void expect(String s) throws XMLStreamException {
        if (!lookingAt(s)) {
            throw error("expected \"" + s + "\"");
        }
        skipOver(s);
    }

    private void skipSpace() throws XMLStreamException {
        while (pos < end && isSpace(buf[pos])) {
            nextChar();
        }
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n';
    }

    private XMLStreamException error(String msg) {
        return new XMLStreamException(msg + " (line " + line + ", column " + column + ")",
                here());
    }

    // ---- the prolog -----------------------------------------------------------------------------

    private void readXmlDeclaration() throws XMLStreamException {
        if (!lookingAt("<?xml") || !hasAhead(5) || !isSpace(peekAt(5))) {
            return;
        }
        skipOver("<?xml");
        skipSpace();
        expect("version");
        skipSpace();
        expect("=");
        skipSpace();
        version = readQuoted();
        skipSpace();
        if (lookingAt("encoding")) {
            skipOver("encoding");
            skipSpace();
            expect("=");
            skipSpace();
            declaredEncoding = readQuoted();
            skipSpace();
        }
        if (lookingAt("standalone")) {
            skipOver("standalone");
            skipSpace();
            expect("=");
            skipSpace();
            String v = readQuoted();
            if (v.equals("yes")) {
                standalone = true;
            } else if (v.equals("no")) {
                standalone = false;
            } else {
                throw error("standalone has to be yes or no, and says \"" + v + "\"");
            }
            standaloneDeclared = true;
            skipSpace();
        }
        expect("?>");
    }

    /** A quoted literal, resolving nothing: it is only valid in the XML declaration. */
    private String readQuoted() throws XMLStreamException {
        char quote = nextChar();
        if (quote != '"' && quote != '\'') {
            throw error("expected a quote");
        }
        StringBuilder sb = new StringBuilder();
        while (pos < end && buf[pos] != quote) {
            sb.append(nextChar());
        }
        if (pos >= end) {
            throw error("unclosed literal");
        }
        nextChar();
        return sb.toString();
    }

    // ---- advancing ------------------------------------------------------------------------------

    public int next() throws XMLStreamException {
        if (closed) {
            throw new XMLStreamException("the reader is already closed");
        }
        if (finished) {
            throw new NoSuchElementException("no events left");
        }
        if (popScopeOnNext) {
            context.closeScope();
            popScopeOnNext = false;
        }
        if (emptyElement) {
            emptyElement = false;
            popScopeOnNext = true;
            depth--;
            attrCount = 0;
            eventType = XMLStreamConstants.END_ELEMENT;
            return eventType;
        }
        if (depth == 0) {
            // Outside the root element there are only comments, instructions, the DOCTYPE and
            // space. The space there is nobody's content, so it generates no event.
            skipSpace();
        }
        location = here();
        if (pos >= end) {
            if (depth > 0) {
                throw error("the document ends with " + depth + " unclosed element(s)");
            }
            if (!sawRoot) {
                throw error("the document has no root element");
            }
            finished = true;
            eventType = XMLStreamConstants.END_DOCUMENT;
            return eventType;
        }
        if (buf[pos] == '<') {
            if (depth == 0 && sawRoot && !lookingAt("<!") && !lookingAt("<?")) {
                throw error("an XML document has a single root element");
            }
            return readMarkup();
        }
        if (depth == 0) {
            throw error("there is text outside the root element");
        }
        return readText();
    }

    private int readMarkup() throws XMLStreamException {
        if (lookingAt("<!--")) {
            return readComment();
        }
        if (lookingAt("<![CDATA[")) {
            return readText();
        }
        if (lookingAt("<!DOCTYPE")) {
            return readDoctype();
        }
        if (lookingAt("<?")) {
            return readPI();
        }
        if (lookingAt("</")) {
            return readEndTag();
        }
        return readStartTag();
    }

    private int readComment() throws XMLStreamException {
        skipOver("<!--");
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= end) {
                throw error("unclosed comment");
            }
            if (lookingAt("-->")) {
                skipOver("-->");
                break;
            }
            sb.append(nextChar());
        }
        text = sb.toString();
        eventType = XMLStreamConstants.COMMENT;
        return eventType;
    }

    private int readPI() throws XMLStreamException {
        skipOver("<?");
        StringBuilder d = new StringBuilder();
        while (pos < end && !isSpace(buf[pos]) && !lookingAt("?>")) {
            d.append(nextChar());
        }
        if (d.length() == 0) {
            throw error("the processing instruction has no target");
        }
        piTarget = d.toString();
        StringBuilder data = new StringBuilder();
        if (pos < end && isSpace(buf[pos])) {
            skipSpace();
            while (true) {
                if (pos >= end) {
                    throw error("unclosed processing instruction");
                }
                if (lookingAt("?>")) {
                    break;
                }
                data.append(nextChar());
            }
        }
        if (!lookingAt("?>")) {
            throw error("unclosed processing instruction");
        }
        skipOver("?>");
        piData = data.toString();
        text = piData;
        eventType = XMLStreamConstants.PROCESSING_INSTRUCTION;
        return eventType;
    }

    /**
     * The whole {@code <!DOCTYPE ...>}, internal subset included, as text.
     *
     * <p>The brackets are counted to know where it really ends: a {@code >} inside the internal
     * subset does not close the declaration.
     */
    private int readDoctype() throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        skipOver("<!DOCTYPE");
        sb.append("<!DOCTYPE");
        int brackets = 0;
        while (true) {
            if (pos >= end) {
                throw error("unclosed DOCTYPE");
            }
            char c = nextChar();
            sb.append(c);
            if (c == '[') {
                brackets++;
            } else if (c == ']') {
                brackets--;
            } else if (c == '>' && brackets <= 0) {
                break;
            }
        }
        text = sb.toString();
        eventType = XMLStreamConstants.DTD;
        return eventType;
    }

    private int readStartTag() throws XMLStreamException {
        expect("<");
        String raw = readName();
        attrCount = 0;
        // The names are resolved after reading all the attributes, because an xmlns declaration in
        // this same tag holds for the name of the element that carries it.
        String[] rawAttr = new String[8];
        String[] rawValue = new String[8];
        int rawCount = 0;
        boolean empty = false;
        while (true) {
            skipSpace();
            if (pos >= end) {
                throw error("unclosed tag");
            }
            if (lookingAt("/>")) {
                skipOver("/>");
                empty = true;
                break;
            }
            if (buf[pos] == '>') {
                nextChar();
                break;
            }
            String an = readName();
            skipSpace();
            expect("=");
            skipSpace();
            String av = readAttributeValue();
            if (rawCount == rawAttr.length) {
                String[] a = new String[rawCount * 2];
                String[] v = new String[rawCount * 2];
                System.arraycopy(rawAttr, 0, a, 0, rawCount);
                System.arraycopy(rawValue, 0, v, 0, rawCount);
                rawAttr = a;
                rawValue = v;
            }
            for (int i = 0; i < rawCount; i++) {
                if (rawAttr[i].equals(an)) {
                    throw error("attribute " + an + " is repeated");
                }
            }
            rawAttr[rawCount] = an;
            rawValue[rawCount] = av;
            rawCount++;
        }

        context.openScope();
        if (namespaceAware) {
            for (int i = 0; i < rawCount; i++) {
                String a = rawAttr[i];
                if (a.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                    context.declare(XMLConstants.DEFAULT_NS_PREFIX, rawValue[i]);
                } else if (a.startsWith("xmlns:")) {
                    String p = a.substring(6);
                    if (p.length() == 0) {
                        throw error("xmlns declaration without a prefix");
                    }
                    if (rawValue[i].length() == 0) {
                        throw error("cannot declare the prefix " + p + " como vacio");
                    }
                    context.declare(p, rawValue[i]);
                }
            }
        }

        currentName = resolveName(raw, true);
        for (int i = 0; i < rawCount; i++) {
            String a = rawAttr[i];
            if (namespaceAware
                    && (a.equals(XMLConstants.XMLNS_ATTRIBUTE) || a.startsWith("xmlns:"))) {
                continue;
            }
            QName q = resolveName(a, false);
            addAttribute(q.getPrefix(), q.getNamespaceURI(), q.getLocalPart(), rawValue[i]);
        }
        for (int i = 0; i < attrCount; i++) {
            for (int j = i + 1; j < attrCount; j++) {
                if (attrLocal[i].equals(attrLocal[j]) && attrUri[i].equals(attrUri[j])) {
                    throw error("two attributes with the same expanded name: " + attrLocal[i]);
                }
            }
        }

        if (depth == stack.length) {
            QName[] bigger = new QName[stack.length * 2];
            System.arraycopy(stack, 0, bigger, 0, stack.length);
            stack = bigger;
        }
        stack[depth] = currentName;
        depth++;
        sawRoot = true;
        emptyElement = empty;
        eventType = XMLStreamConstants.START_ELEMENT;
        return eventType;
    }

    private void addAttribute(String prefix, String uri, String local, String value) {
        if (attrCount == attrLocal.length) {
            int m = attrCount * 2;
            String[] p = new String[m];
            String[] u = new String[m];
            String[] l = new String[m];
            String[] v = new String[m];
            System.arraycopy(attrPrefix, 0, p, 0, attrCount);
            System.arraycopy(attrUri, 0, u, 0, attrCount);
            System.arraycopy(attrLocal, 0, l, 0, attrCount);
            System.arraycopy(attrValue, 0, v, 0, attrCount);
            attrPrefix = p;
            attrUri = u;
            attrLocal = l;
            attrValue = v;
        }
        attrPrefix[attrCount] = prefix;
        attrUri[attrCount] = uri;
        attrLocal[attrCount] = local;
        attrValue[attrCount] = value;
        attrCount++;
    }

    /**
     * From a raw name to the {@link QName} that corresponds to it.
     *
     * <p>{@code forElement} decides the asymmetry of the Namespaces specification: an element
     * without a prefix falls into the default namespace, an attribute without a prefix does not.
     */
    private QName resolveName(String raw, boolean forElement) throws XMLStreamException {
        if (!namespaceAware) {
            return new QName(XMLConstants.NULL_NS_URI, raw, XMLConstants.DEFAULT_NS_PREFIX);
        }
        int colon = raw.indexOf(':');
        if (colon < 0) {
            String uri = XMLConstants.NULL_NS_URI;
            if (forElement) {
                uri = context.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
            }
            return new QName(uri, raw, XMLConstants.DEFAULT_NS_PREFIX);
        }
        String p = raw.substring(0, colon);
        String l = raw.substring(colon + 1);
        if (l.length() == 0 || l.indexOf(':') >= 0) {
            throw error("malformed qualified name: " + raw);
        }
        String uri = context.getNamespaceURI(p);
        if (uri == null || uri.length() == 0) {
            throw error("the prefix " + p + " is not declared");
        }
        return new QName(uri, l, p);
    }

    private String readName() throws XMLStreamException {
        if (pos >= end || !Names.isNameStart(buf[pos])) {
            throw error("expected a name");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(nextChar());
        while (pos < end && Names.isNamePart(buf[pos])) {
            sb.append(nextChar());
        }
        return sb.toString();
    }

    /**
     * An attribute value, with the entities already resolved.
     *
     * <p>Literal whitespace --a tab or line break written as is-- turns into an ordinary space,
     * which is the normalization XML dictates for CDATA-type attributes. Whitespace coming from a
     * character reference is <b>not</b> normalized: writing {@code &#10;} is exactly the way of
     * putting in a line break that survives, and confusing them makes a document lose data on a
     * round trip.
     */
    private String readAttributeValue() throws XMLStreamException {
        char quote = nextChar();
        if (quote != '"' && quote != '\'') {
            throw error("an attribute value goes in quotes");
        }
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= end) {
                throw error("unclosed attribute value");
            }
            char c = buf[pos];
            if (c == quote) {
                nextChar();
                break;
            }
            if (c == '<') {
                throw error("an attribute value cannot contain '<'");
            }
            if (c == '&') {
                String r = readReference();
                if (r == null) {
                    throw error("an entity cannot be left unexpanded in an attribute");
                }
                sb.append(r);
                continue;
            }
            nextChar();
            if (c == '\t' || c == '\n') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private int readEndTag() throws XMLStreamException {
        skipOver("</");
        String raw = readName();
        skipSpace();
        expect(">");
        if (depth == 0) {
            throw error("closing " + raw + " and nothing is open");
        }
        QName openName = stack[depth - 1];
        QName closeName = resolveName(raw, true);
        if (!openName.equals(closeName) || !openName.getPrefix().equals(closeName.getPrefix())) {
            throw error("opened " + Names.written(openName) + " and closing " + raw);
        }
        currentName = openName;
        depth--;
        attrCount = 0;
        popScopeOnNext = true;
        eventType = XMLStreamConstants.END_ELEMENT;
        return eventType;
    }

    /**
     * A stretch of text, perhaps with CDATA sections and references inside.
     *
     * <p>With {@link XMLInputFactory#IS_COALESCING} off it is cut at each markup boundary, which is
     * what the specification allows; on, it keeps gathering while what comes is also text.
     */
    private int readText() throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        boolean sawText = false;
        while (true) {
            if (lookingAt("<![CDATA[")) {
                if (sawText && !coalescing) {
                    break;
                }
                skipOver("<![CDATA[");
                while (true) {
                    if (pos >= end) {
                        throw error("unclosed CDATA section");
                    }
                    if (lookingAt("]]>")) {
                        skipOver("]]>");
                        break;
                    }
                    sb.append(nextChar());
                }
                if (!coalescing) {
                    break;
                }
                continue;
            }
            if (pos >= end || buf[pos] == '<') {
                break;
            }
            if (buf[pos] == '&') {
                int before = pos;
                String r = readReference();
                if (r == null) {
                    // An entity that has to be delivered unexpanded. If text has already been
                    // gathered, the text goes first and the reference comes out on the next next().
                    if (sb.length() > 0) {
                        pos = before;
                        break;
                    }
                    eventType = XMLStreamConstants.ENTITY_REFERENCE;
                    text = null;
                    return eventType;
                }
                sb.append(r);
                sawText = true;
                continue;
            }
            sb.append(nextChar());
            sawText = true;
        }
        text = sb.toString();
        // A CDATA section is delivered as CHARACTERS, not as CDATA, and that has to be said because
        // it looks like a mistake: `XMLStreamConstants.CDATA` exists and no reader here emits it.
        //
        // It is what JDK 25 does with its default reader --checked by running the same document
        // with `javax.xml.stream` there: it returns 4 and `isCData()` is false-- and it is what the
        // specification allows: reporting CDATA as an event of its own is OPTIONAL and tied to a
        // factory property that neither the JDK nor we implement. The content arrives all the same,
        // raw and without the wrapper, which is what the document says.
        //
        // This branch returned CDATA until the behaviour test was run against the JDK and did not
        // match. The wrong expectation was ours.
        eventType = XMLStreamConstants.CHARACTERS;
        return eventType;
    }

    /**
     * A reference {@code &...;}.
     *
     * @return the replacement text, or null if it is an entity to be delivered unexpanded; in that
     *     case it leaves the reference consumed and the name in {@link #entityName}
     */
    private String readReference() throws XMLStreamException {
        nextChar();
        if (pos < end && buf[pos] == '#') {
            nextChar();
            int radix = 10;
            if (pos < end && (buf[pos] == 'x' || buf[pos] == 'X')) {
                nextChar();
                radix = 16;
            }
            int value = 0;
            int digits = 0;
            while (pos < end && buf[pos] != ';') {
                int d = Character.digit(nextChar(), radix);
                if (d < 0) {
                    throw error("malformed character reference");
                }
                value = value * radix + d;
                digits++;
                if (value > 0x10FFFF) {
                    throw error("character reference outside the Unicode range");
                }
            }
            if (digits == 0) {
                throw error("empty character reference");
            }
            expect(";");
            if (!isLegalChar(value)) {
                throw error("character U+" + Integer.toHexString(value)
                        + " cannot appear in an XML document");
            }
            return new String(Character.toChars(value));
        }
        String n = readName();
        expect(";");
        if (n.equals("lt")) {
            return "<";
        }
        if (n.equals("gt")) {
            return ">";
        }
        if (n.equals("amp")) {
            return "&";
        }
        if (n.equals("quot")) {
            return "\"";
        }
        if (n.equals("apos")) {
            return "'";
        }
        if (replacingEntityRefs) {
            throw error("the entity " + n + " is undeclared, and this library reads no DTD");
        }
        entityName = n;
        return null;
    }

    private static boolean isLegalChar(int c) {
        if (c == 0x9 || c == 0xA || c == 0xD) {
            return true;
        }
        if (c >= 0x20 && c <= 0xD7FF) {
            return true;
        }
        if (c >= 0xE000 && c <= 0xFFFD) {
            return true;
        }
        if (c >= 0x10000 && c <= 0x10FFFF) {
            return true;
        }
        return false;
    }

    // ---- the cursor API -------------------------------------------------------------------------

    public boolean hasNext() throws XMLStreamException {
        return !finished;
    }

    public void close() throws XMLStreamException {
        closed = true;
    }

    public int getEventType() {
        return eventType;
    }

    public Object getProperty(String name) {
        if (name == null) {
            throw new IllegalArgumentException("the property name cannot be null");
        }
        if (name.equals(XMLInputFactory.IS_COALESCING)) {
            return Boolean.valueOf(coalescing);
        }
        if (name.equals(XMLInputFactory.IS_NAMESPACE_AWARE)) {
            return Boolean.valueOf(namespaceAware);
        }
        if (name.equals(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES)) {
            return Boolean.valueOf(replacingEntityRefs);
        }
        if (name.equals(XMLInputFactory.IS_VALIDATING)
                || name.equals(XMLInputFactory.SUPPORT_DTD)
                || name.equals(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("propiedad desconocida: " + name);
    }

    public void require(int type, String namespaceURI, String localName)
            throws XMLStreamException {
        if (type != eventType) {
            throw new XMLStreamException("expected event " + type + " and it is " + eventType,
                    getLocation());
        }
        if (namespaceURI != null) {
            if (currentName == null || !namespaceURI.equals(currentName.getNamespaceURI())) {
                throw new XMLStreamException("expected namespace " + namespaceURI,
                        getLocation());
            }
        }
        if (localName != null) {
            if (currentName == null || !localName.equals(currentName.getLocalPart())) {
                throw new XMLStreamException("expected local name " + localName,
                        getLocation());
            }
        }
    }

    public String getElementText() throws XMLStreamException {
        if (eventType != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException(
                    "getElementText() is called at START_ELEMENT", getLocation());
        }
        StringBuilder sb = new StringBuilder();
        int t = next();
        while (t != XMLStreamConstants.END_ELEMENT) {
            if (t == XMLStreamConstants.CHARACTERS || t == XMLStreamConstants.CDATA
                    || t == XMLStreamConstants.SPACE
                    || t == XMLStreamConstants.ENTITY_REFERENCE) {
                if (text != null) {
                    sb.append(text);
                }
            } else if (t == XMLStreamConstants.START_ELEMENT) {
                throw new XMLStreamException(
                        "the element has children, so it is not text-only", getLocation());
            } else if (t == XMLStreamConstants.END_DOCUMENT) {
                throw new XMLStreamException("the document ends inside the element",
                        getLocation());
            }
            t = next();
        }
        return sb.toString();
    }

    public int nextTag() throws XMLStreamException {
        int t = next();
        while (true) {
            if ((t == XMLStreamConstants.CHARACTERS || t == XMLStreamConstants.CDATA)
                    && isWhiteSpace()) {
                t = next();
                continue;
            }
            if (t == XMLStreamConstants.SPACE
                    || t == XMLStreamConstants.PROCESSING_INSTRUCTION
                    || t == XMLStreamConstants.COMMENT) {
                t = next();
                continue;
            }
            break;
        }
        if (t != XMLStreamConstants.START_ELEMENT && t != XMLStreamConstants.END_ELEMENT) {
            throw new XMLStreamException(
                    "expected a tag and got event " + t, getLocation());
        }
        return t;
    }

    // ---- names -----------------------------------------------------------------------------

    private void requireElement() {
        if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
            throw new IllegalStateException("there is no element at the current event");
        }
    }

    public QName getName() {
        requireElement();
        return currentName;
    }

    public String getLocalName() {
        if (eventType == XMLStreamConstants.ENTITY_REFERENCE) {
            return entityName;
        }
        requireElement();
        return currentName.getLocalPart();
    }

    public boolean hasName() {
        return eventType == XMLStreamConstants.START_ELEMENT || eventType == XMLStreamConstants.END_ELEMENT;
    }

    public String getNamespaceURI() {
        if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
            return null;
        }
        String u = currentName.getNamespaceURI();
        if (u.length() == 0) {
            return null;
        }
        return u;
    }

    public String getPrefix() {
        if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
            return null;
        }
        String p = currentName.getPrefix();
        if (p.length() == 0) {
            return null;
        }
        return p;
    }

    // ---- attributes --------------------------------------------------------------------------

    private void requireStartElement() {
        if (eventType != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException("attributes are only at START_ELEMENT");
        }
    }

    public int getAttributeCount() {
        requireStartElement();
        return attrCount;
    }

    public QName getAttributeName(int index) {
        requireStartElement();
        checkRange(index, attrCount);
        return new QName(attrUri[index], attrLocal[index], attrPrefix[index]);
    }

    public String getAttributeNamespace(int index) {
        requireStartElement();
        checkRange(index, attrCount);
        if (attrUri[index].length() == 0) {
            return null;
        }
        return attrUri[index];
    }

    public String getAttributeLocalName(int index) {
        requireStartElement();
        checkRange(index, attrCount);
        return attrLocal[index];
    }

    public String getAttributePrefix(int index) {
        requireStartElement();
        checkRange(index, attrCount);
        if (attrPrefix[index].length() == 0) {
            return null;
        }
        return attrPrefix[index];
    }

    public String getAttributeType(int index) {
        requireStartElement();
        checkRange(index, attrCount);
        return "CDATA";
    }

    public String getAttributeValue(int index) {
        requireStartElement();
        checkRange(index, attrCount);
        return attrValue[index];
    }

    public boolean isAttributeSpecified(int index) {
        requireStartElement();
        checkRange(index, attrCount);
        return true;
    }

    public String getAttributeValue(String namespaceURI, String localName) {
        requireStartElement();
        for (int i = 0; i < attrCount; i++) {
            if (!attrLocal[i].equals(localName)) {
                continue;
            }
            if (namespaceURI == null || namespaceURI.equals(attrUri[i])) {
                return attrValue[i];
            }
        }
        return null;
    }

    private static void checkRange(int i, int n) {
        if (i < 0 || i >= n) {
            throw new IndexOutOfBoundsException("index " + i + " of " + n);
        }
    }

    // ---- namespaces -----------------------------------------------------------------------------

    public int getNamespaceCount() {
        requireElement();
        return context.declaredInScope();
    }

    public String getNamespacePrefix(int index) {
        requireElement();
        checkRange(index, context.declaredInScope());
        String p = context.prefixes[context.indexInScope(index)];
        if (p.length() == 0) {
            return null;
        }
        return p;
    }

    public String getNamespaceURI(int index) {
        requireElement();
        checkRange(index, context.declaredInScope());
        return context.uris[context.indexInScope(index)];
    }

    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("the prefix cannot be null");
        }
        String u = context.getNamespaceURI(prefix);
        if (u == null || u.length() == 0) {
            return null;
        }
        return u;
    }

    public NamespaceContext getNamespaceContext() {
        return context.snapshot();
    }

    // ---- text ---------------------------------------------------------------------------------

    public boolean hasText() {
        return eventType == XMLStreamConstants.CHARACTERS || eventType == XMLStreamConstants.CDATA
                || eventType == XMLStreamConstants.COMMENT || eventType == XMLStreamConstants.SPACE
                || eventType == XMLStreamConstants.ENTITY_REFERENCE || eventType == XMLStreamConstants.DTD;
    }

    private void requireText() {
        if (!hasText()) {
            throw new IllegalStateException("the current event has no text");
        }
    }

    public String getText() {
        requireText();
        return text;
    }

    public char[] getTextCharacters() {
        requireText();
        if (text == null) {
            return new char[0];
        }
        return text.toCharArray();
    }

    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
            throws XMLStreamException {
        requireText();
        if (target == null) {
            throw new NullPointerException("the destination cannot be null");
        }
        String t = text;
        if (t == null) {
            t = "";
        }
        if (sourceStart < 0 || sourceStart > t.length() || length < 0 || targetStart < 0
                || targetStart + length > target.length) {
            throw new IndexOutOfBoundsException("the bounds do not fit");
        }
        int n = t.length() - sourceStart;
        if (n > length) {
            n = length;
        }
        t.getChars(sourceStart, sourceStart + n, target, targetStart);
        return n;
    }

    public int getTextStart() {
        requireText();
        return 0;
    }

    public int getTextLength() {
        requireText();
        if (text == null) {
            return 0;
        }
        return text.length();
    }

    public boolean isWhiteSpace() {
        if (eventType != XMLStreamConstants.CHARACTERS && eventType != XMLStreamConstants.SPACE
                && eventType != XMLStreamConstants.CDATA) {
            return false;
        }
        if (text == null) {
            return false;
        }
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return false;
            }
        }
        return true;
    }

    // ---- about the document ---------------------------------------------------------------------

    public boolean isStartElement() {
        return eventType == XMLStreamConstants.START_ELEMENT;
    }

    public boolean isEndElement() {
        return eventType == XMLStreamConstants.END_ELEMENT;
    }

    public boolean isCharacters() {
        return eventType == XMLStreamConstants.CHARACTERS;
    }

    public String getEncoding() {
        return sourceEncoding;
    }

    public String getCharacterEncodingScheme() {
        return declaredEncoding;
    }

    public String getVersion() {
        return version;
    }

    public boolean isStandalone() {
        return standalone;
    }

    public boolean standaloneSet() {
        return standaloneDeclared;
    }

    public String getPITarget() {
        if (eventType != XMLStreamConstants.PROCESSING_INSTRUCTION) {
            return null;
        }
        return piTarget;
    }

    public String getPIData() {
        if (eventType != XMLStreamConstants.PROCESSING_INSTRUCTION) {
            return null;
        }
        return piData;
    }

    public Location getLocation() {
        if (location == null) {
            return KajiLocation.NONE;
        }
        return location;
    }
}
