package javax.xml.stream;

import java.io.IOException;
import java.io.Reader;
import java.util.NoSuchElementException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 * El analizador de XML 1.0 de esta biblioteca, en forma de cursor.
 *
 * <h2>Que reconoce</h2>
 *
 * <p>Documentos bien formados con espacios de nombres: declaracion XML, instrucciones de
 * procesamiento, comentarios, {@code <!DOCTYPE>} (que se entrega crudo, sin interpretar), elementos,
 * atributos, texto, secciones CDATA, las cinco entidades predefinidas y las referencias de caracter
 * decimales y hexadecimales.
 *
 * <p>Lo que no hace esta escrito en {@link XMLInputFactory}: no interpreta el DTD, no resuelve
 * entidades externas y no valida.
 *
 * <h2>Lee todo de entrada, y por que</h2>
 *
 * <p>El constructor consume el {@link Reader} entero antes de devolver el primer evento. Un parser
 * de verdad incremental tiene que manejar tokens partidos entre dos llenados del buffer, que es de
 * donde salen los errores mas dificiles de encontrar de un analizador; aca el costo es memoria
 * proporcional al documento y a cambio no existe esa clase de bug.
 *
 * <p>La consecuencia visible: cerrar el lector a la mitad no ahorra lectura, porque ya se leyo. Y
 * un documento que no termina nunca --un socket-- no se puede procesar de a poco. Es una limitacion
 * real de esta implementacion, no de la API.
 *
 * <h2>Los finales de linea se normalizan una sola vez</h2>
 *
 * <p>XML manda convertir {@code \r\n} y {@code \r} en {@code \n} antes de analizar. Se hace sobre el
 * buffer completo al cargarlo, con lo cual el resto del codigo --y el conteo de lineas-- no vuelve a
 * pensar en el tema.
 */
final class KajiStreamReader implements XMLStreamReader {

    // ---- configuracion ----------------------------------------------------------------------

    private final boolean coalescing;
    private final boolean namespaceAware;
    private final boolean replacingEntityRefs;
    private final String systemId;
    private final String sourceEncoding;

    // ---- el texto ---------------------------------------------------------------------------

    private final char[] buf;
    private final int end;
    private int pos;
    private int line = 1;
    private int column = 1;

    // ---- el evento actual -------------------------------------------------------------------

    private int eventType = XMLStreamConstants.START_DOCUMENT;
    private String text;
    private QName currentName;
    private String piTarget;
    private String piData;
    private String entityName;
    private KajiLocation location;

    // ---- el prologo -------------------------------------------------------------------------

    private String version = "1.0";
    private String declaredEncoding;
    private boolean standalone;
    private boolean standaloneDeclared;

    // ---- atributos del elemento actual -------------------------------------------------------

    private String[] attrPrefix = new String[8];
    private String[] attrUri = new String[8];
    private String[] attrLocal = new String[8];
    private String[] attrValue = new String[8];
    private int attrCount;

    // ---- la estructura ----------------------------------------------------------------------

    private final KajiNsContext context = new KajiNsContext();
    private QName[] stack = new QName[16];
    private int depth;
    private boolean emptyElement;
    private boolean popScopeOnNext;
    private boolean finished;
    private boolean closed;
    private boolean sawRoot;

    // ---- construccion -----------------------------------------------------------------------

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

    /** Lee el flujo entero y normaliza los finales de linea de una sola pasada. */
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
            throw new XMLStreamException("no se pudo leer la entrada", e);
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

    // ---- lectura de caracteres ---------------------------------------------------------------

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
            throw error("el documento se corta antes de tiempo");
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
            throw error("se esperaba \"" + s + "\"");
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
        return new XMLStreamException(msg + " (linea " + line + ", columna " + column + ")",
                here());
    }

    // ---- el prologo -------------------------------------------------------------------------

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
                throw error("standalone tiene que ser yes o no, y dice \"" + v + "\"");
            }
            standaloneDeclared = true;
            skipSpace();
        }
        expect("?>");
    }

    /** Un literal entre comillas, sin resolver nada: solo vale en la declaracion XML. */
    private String readQuoted() throws XMLStreamException {
        char quote = nextChar();
        if (quote != '"' && quote != '\'') {
            throw error("se esperaba una comilla");
        }
        StringBuilder sb = new StringBuilder();
        while (pos < end && buf[pos] != quote) {
            sb.append(nextChar());
        }
        if (pos >= end) {
            throw error("literal sin cerrar");
        }
        nextChar();
        return sb.toString();
    }

    // ---- el avance --------------------------------------------------------------------------

    public int next() throws XMLStreamException {
        if (closed) {
            throw new XMLStreamException("el lector ya esta cerrado");
        }
        if (finished) {
            throw new NoSuchElementException("no quedan eventos");
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
            // Fuera del elemento raiz solo hay comentarios, instrucciones, el DOCTYPE y espacio.
            // El espacio de ahi no es contenido de nadie, asi que no genera evento.
            skipSpace();
        }
        location = here();
        if (pos >= end) {
            if (depth > 0) {
                throw error("el documento termina con " + depth + " elemento(s) sin cerrar");
            }
            if (!sawRoot) {
                throw error("el documento no tiene elemento raiz");
            }
            finished = true;
            eventType = XMLStreamConstants.END_DOCUMENT;
            return eventType;
        }
        if (buf[pos] == '<') {
            if (depth == 0 && sawRoot && !lookingAt("<!") && !lookingAt("<?")) {
                throw error("un documento XML tiene un solo elemento raiz");
            }
            return readMarkup();
        }
        if (depth == 0) {
            throw error("hay texto fuera del elemento raiz");
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
                throw error("comentario sin cerrar");
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
            throw error("la instruccion de procesamiento no tiene destino");
        }
        piTarget = d.toString();
        StringBuilder data = new StringBuilder();
        if (pos < end && isSpace(buf[pos])) {
            skipSpace();
            while (true) {
                if (pos >= end) {
                    throw error("instruccion de procesamiento sin cerrar");
                }
                if (lookingAt("?>")) {
                    break;
                }
                data.append(nextChar());
            }
        }
        if (!lookingAt("?>")) {
            throw error("instruccion de procesamiento sin cerrar");
        }
        skipOver("?>");
        piData = data.toString();
        text = piData;
        eventType = XMLStreamConstants.PROCESSING_INSTRUCTION;
        return eventType;
    }

    /**
     * El {@code <!DOCTYPE ...>} entero, incluido el subconjunto interno, como texto.
     *
     * <p>Se cuentan los corchetes para saber donde termina de verdad: un {@code >} adentro del
     * subconjunto interno no cierra la declaracion.
     */
    private int readDoctype() throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        skipOver("<!DOCTYPE");
        sb.append("<!DOCTYPE");
        int brackets = 0;
        while (true) {
            if (pos >= end) {
                throw error("DOCTYPE sin cerrar");
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
        // Los nombres se resuelven despues de leer todos los atributos, porque una declaracion
        // xmlns de esta misma etiqueta vale para el nombre del elemento que la lleva.
        String[] rawAttr = new String[8];
        String[] rawValue = new String[8];
        int rawCount = 0;
        boolean empty = false;
        while (true) {
            skipSpace();
            if (pos >= end) {
                throw error("etiqueta sin cerrar");
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
                    throw error("el atributo " + an + " esta repetido");
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
                        throw error("declaracion xmlns sin prefijo");
                    }
                    if (rawValue[i].length() == 0) {
                        throw error("no se puede declarar el prefijo " + p + " como vacio");
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
                    throw error("dos atributos con el mismo nombre expandido: " + attrLocal[i]);
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
     * De un nombre crudo al {@link QName} que le corresponde.
     *
     * <p>{@code deElemento} decide la asimetria de la especificacion de Namespaces: un elemento sin
     * prefijo cae en el espacio de nombres por omision, un atributo sin prefijo no.
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
            throw error("nombre calificado mal formado: " + raw);
        }
        String uri = context.getNamespaceURI(p);
        if (uri == null || uri.length() == 0) {
            throw error("el prefijo " + p + " no esta declarado");
        }
        return new QName(uri, l, p);
    }

    private String readName() throws XMLStreamException {
        if (pos >= end || !Names.isNameStart(buf[pos])) {
            throw error("se esperaba un nombre");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(nextChar());
        while (pos < end && Names.isNamePart(buf[pos])) {
            sb.append(nextChar());
        }
        return sb.toString();
    }

    /**
     * Un valor de atributo, con las entidades ya resueltas.
     *
     * <p>El espacio literal --tabulador o salto de linea escrito tal cual-- se convierte en un
     * espacio comun, que es la normalizacion que manda XML para los atributos de tipo CDATA. El que
     * viene de una referencia de caracter <b>no</b> se normaliza: escribir {@code &#10;} es
     * exactamente la forma de meter un salto de linea que sobreviva, y confundirlos hace que un
     * documento pierda datos al ida y vuelta.
     */
    private String readAttributeValue() throws XMLStreamException {
        char quote = nextChar();
        if (quote != '"' && quote != '\'') {
            throw error("el valor de un atributo va entre comillas");
        }
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= end) {
                throw error("valor de atributo sin cerrar");
            }
            char c = buf[pos];
            if (c == quote) {
                nextChar();
                break;
            }
            if (c == '<') {
                throw error("un valor de atributo no puede contener '<'");
            }
            if (c == '&') {
                String r = readReference();
                if (r == null) {
                    throw error("en un atributo no se puede dejar una entidad sin expandir");
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
            throw error("se cierra " + raw + " y no hay nada abierto");
        }
        QName openName = stack[depth - 1];
        QName closeName = resolveName(raw, true);
        if (!openName.equals(closeName) || !openName.getPrefix().equals(closeName.getPrefix())) {
            throw error("se abrio " + Names.written(openName) + " y se cierra " + raw);
        }
        currentName = openName;
        depth--;
        attrCount = 0;
        popScopeOnNext = true;
        eventType = XMLStreamConstants.END_ELEMENT;
        return eventType;
    }

    /**
     * Un tramo de texto, quiza con secciones CDATA y referencias adentro.
     *
     * <p>Con {@link XMLInputFactory#IS_COALESCING} apagado se corta en cada frontera de marcado,
     * que es lo que la especificacion permite; encendido, sigue juntando mientras lo que venga
     * tambien sea texto.
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
                        throw error("seccion CDATA sin cerrar");
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
                    // Una entidad que hay que entregar sin expandir. Si ya juntamos texto, el
                    // texto va primero y la referencia sale en el proximo next().
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
        // Una seccion CDATA se entrega como CHARACTERS, no como CDATA, y eso hay que decirlo porque
        // parece un error: `XMLStreamConstants.CDATA` existe y ningun lector de esta casa lo emite.
        //
        // Es lo que hace el JDK 25 con su lector por omision --se comprobo corriendo el mismo
        // documento con `javax.xml.stream` de alla: devuelve 4 y `isCData()` en false-- y es lo que
        // la especificacion permite: reportar CDATA como evento propio es OPCIONAL y esta atado a
        // una propiedad de fabrica que ni el JDK ni nosotros implementamos. El contenido llega
        // igual, crudo y sin la envoltura, que es lo que el documento dice.
        //
        // Esta rama devolvia CDATA hasta que la prueba de comportamiento se corrio contra el JDK y
        // no coincidio. La expectativa equivocada era la nuestra.
        eventType = XMLStreamConstants.CHARACTERS;
        return eventType;
    }

    /**
     * Una referencia {@code &...;}.
     *
     * @return el texto de reemplazo, o null si es una entidad que hay que entregar sin expandir; en
     *     ese caso deja consumida la referencia y el nombre en {@link #entidad}
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
                    throw error("referencia de caracter mal formada");
                }
                value = value * radix + d;
                digits++;
                if (value > 0x10FFFF) {
                    throw error("referencia de caracter fuera del rango de Unicode");
                }
            }
            if (digits == 0) {
                throw error("referencia de caracter vacia");
            }
            expect(";");
            if (!isLegalChar(value)) {
                throw error("el caracter U+" + Integer.toHexString(value)
                        + " no puede aparecer en un documento XML");
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
            throw error("la entidad " + n + " no esta declarada, y esta biblioteca no lee el DTD");
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

    // ---- la API del cursor -------------------------------------------------------------------

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
            throw new IllegalArgumentException("el nombre de la propiedad no puede ser null");
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
            throw new XMLStreamException("se esperaba el evento " + type + " y es " + eventType,
                    getLocation());
        }
        if (namespaceURI != null) {
            if (currentName == null || !namespaceURI.equals(currentName.getNamespaceURI())) {
                throw new XMLStreamException("se esperaba el espacio de nombres " + namespaceURI,
                        getLocation());
            }
        }
        if (localName != null) {
            if (currentName == null || !localName.equals(currentName.getLocalPart())) {
                throw new XMLStreamException("se esperaba el nombre local " + localName,
                        getLocation());
            }
        }
    }

    public String getElementText() throws XMLStreamException {
        if (eventType != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException(
                    "getElementText() se llama parado en START_ELEMENT", getLocation());
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
                        "el elemento tiene hijos, asi que no tiene solo texto", getLocation());
            } else if (t == XMLStreamConstants.END_DOCUMENT) {
                throw new XMLStreamException("el documento termina dentro del elemento",
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
                    "se esperaba una etiqueta y vino el evento " + t, getLocation());
        }
        return t;
    }

    // ---- nombres ---------------------------------------------------------------------------

    private void requireElement() {
        if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
            throw new IllegalStateException("no hay un elemento en el evento actual");
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

    // ---- atributos ---------------------------------------------------------------------------

    private void requireStartElement() {
        if (eventType != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException("los atributos solo estan en START_ELEMENT");
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
            throw new IndexOutOfBoundsException("indice " + i + " de " + n);
        }
    }

    // ---- espacios de nombres ------------------------------------------------------------------

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
            throw new IllegalArgumentException("el prefijo no puede ser null");
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

    // ---- texto --------------------------------------------------------------------------------

    public boolean hasText() {
        return eventType == XMLStreamConstants.CHARACTERS || eventType == XMLStreamConstants.CDATA
                || eventType == XMLStreamConstants.COMMENT || eventType == XMLStreamConstants.SPACE
                || eventType == XMLStreamConstants.ENTITY_REFERENCE || eventType == XMLStreamConstants.DTD;
    }

    private void requireText() {
        if (!hasText()) {
            throw new IllegalStateException("el evento actual no tiene texto");
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
            throw new NullPointerException("el destino no puede ser null");
        }
        String t = text;
        if (t == null) {
            t = "";
        }
        if (sourceStart < 0 || sourceStart > t.length() || length < 0 || targetStart < 0
                || targetStart + length > target.length) {
            throw new IndexOutOfBoundsException("los limites no entran");
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

    // ---- lo del documento ---------------------------------------------------------------------

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
