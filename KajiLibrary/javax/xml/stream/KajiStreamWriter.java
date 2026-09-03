package javax.xml.stream;

import java.io.IOException;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * El escritor de XML de esta biblioteca, en forma de cursor.
 *
 * <h2>La etiqueta abierta</h2>
 *
 * <p>Toda la maquinaria gira alrededor de un estado: {@code <a} escrito y el {@code >} todavia no,
 * porque puede venir un atributo. Cualquier cosa que no sea un atributo o una declaracion cierra la
 * etiqueta primero. Es lo que permite que la API tenga {@code writeStartElement} y
 * {@code writeAttribute} como llamadas separadas sin que el llamador tenga que avisar cuando
 * termino de poner atributos.
 *
 * <h2>Los dos modos de espacios de nombres</h2>
 *
 * <p>Con {@link XMLOutputFactory#IS_REPAIRING_NAMESPACES} apagado el escritor escribe lo que se le
 * dice: si se usa un prefijo que nadie declaro, sale un documento mal formado, y es responsabilidad
 * del llamador. Encendido, antes de escribir un nombre calificado se fija si su espacio de nombres
 * esta en alcance y, si no, emite la declaracion --inventando un prefijo si hace falta--.
 *
 * <p>Lo que el modo reparador no hace es adivinar intenciones: si se pide explicitamente un
 * prefijo, ese se usa; lo que se agrega es la declaracion que faltaba.
 *
 * <h2>Que no comprueba</h2>
 *
 * <p>No verifica que el documento tenga un solo elemento raiz, ni que los nombres sean nombres XML
 * validos, ni que el texto de un comentario no contenga {@code --}. Un escritor que valida todo eso
 * es util, pero cuesta en el camino caliente y la especificacion no lo pide; lo que si se
 * comprueba es lo estructural --cerrar un elemento que no esta abierto, escribir un atributo fuera
 * de una etiqueta-- porque eso produce basura silenciosa en vez de un error.
 */
final class KajiStreamWriter implements XMLStreamWriter {

    private final Writer w;
    private final boolean repairing;

    private final KajiNsContext ctx = new KajiNsContext();
    private NamespaceContext rootCtx;

    /** Prefijos declarados con {@code setPrefix} que todavia no tienen elemento donde vivir. */
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

    // ---- plomeria ---------------------------------------------------------------------------

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
                    "esto solo se puede escribir dentro de una etiqueta de apertura");
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

    /** El prefijo en alcance para un URI, mirando primero lo propio y despues el contexto puesto. */
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

    // ---- elementos --------------------------------------------------------------------------

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
                            "el espacio de nombres " + uri + " no tiene prefijo en alcance; "
                                    + "declaralo antes, o encende isRepairingNamespaces");
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
                    // Sin reparacion la declaracion la escribe el llamador; el binding se anota
                    // igual para que getPrefix() diga la verdad.
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
     * Cierra el elemento abierto mas reciente.
     *
     * <p>Si lo ultimo que se escribio fue un {@link #writeEmptyElement}, esta llamada cierra al
     * elemento QUE LO CONTIENE, no al vacio -- el vacio ya se cierra solo con su `/>`. Es lo que
     * hace el JDK: `writeStartElement("r"); writeEmptyElement("e"); writeEndElement();` produce
     * `<r><e/></r>`.
     *
     * <p>Esto tiraba una excepcion hasta que la prueba de comportamiento se corrio contra el JDK 25
     * y no coincidio. Parecia razonable --nadie "cierra" un elemento vacio-- pero lee mal la
     * llamada: `writeEndElement` no dice cual cierra, cierra el que este abierto, y despues de un
     * vacio el que esta abierto es el de afuera.
     */
    public void writeEndElement() throws XMLStreamException {
        closeTag();
        if (depth == 0) {
            throw new XMLStreamException("no hay ningun elemento abierto");
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

    // ---- atributos y declaraciones -----------------------------------------------------------

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
            // Un atributo sin prefijo no queda en el espacio de nombres por omision, asi que aca
            // hace falta uno de verdad; en modo reparador se inventa.
            if (!repairing) {
                throw new XMLStreamException(
                        "el espacio de nombres " + uri + " no tiene prefijo en alcance para un "
                                + "atributo; declaralo antes, o encende isRepairingNamespaces");
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

    // ---- contenido --------------------------------------------------------------------------

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

    // ---- estado -----------------------------------------------------------------------------

    public String getPrefix(String uri) throws XMLStreamException {
        return prefixFor(uri);
    }

    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        if (prefix == null) {
            throw new XMLStreamException("el prefijo no puede ser null");
        }
        if (uri == null) {
            throw new XMLStreamException("el espacio de nombres no puede ser null");
        }
        addPending(prefix, uri);
    }

    public void setDefaultNamespace(String uri) throws XMLStreamException {
        if (uri == null) {
            throw new XMLStreamException("el espacio de nombres no puede ser null");
        }
        addPending(XMLConstants.DEFAULT_NS_PREFIX, uri);
    }

    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        if (depth > 0 || tagOpen) {
            throw new XMLStreamException(
                    "el contexto de espacios de nombres se pone antes de escribir la raiz");
        }
        rootCtx = context;
    }

    public NamespaceContext getNamespaceContext() {
        return ctx.snapshot();
    }

    public Object getProperty(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("el nombre de la propiedad no puede ser null");
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
