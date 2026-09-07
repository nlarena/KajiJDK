package javax.swing.text.html.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

import javax.swing.text.ChangedCharSetException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;

/**
 * El analizador de HTML guiado por una DTD.
 *
 * <h2>Por que hace falta la DTD</h2>
 *
 * <p>El HTML que se escribe de verdad tiene etiquetas sin cerrar. Un analizador que solo mirara lo
 * escrito no podria armar el arbol de <code>&lt;p&gt;uno&lt;p&gt;dos</code>, porque nadie cerro el
 * primer parrafo. La DTD dice que un parrafo puede cerrarse solo, y con eso el analizador cierra lo
 * que falte antes de abrir lo que sigue.
 *
 * <p>Tres reglas alcanzan para casi todo, y las tres salen de la DTD:
 *
 * <ul>
 * <li>Si el elemento actual no acepta lo que viene y su cierre se puede omitir, se cierra.
 * <li>Si lo que viene no puede ir suelto, se abre lo que le falta ({@link Element#omitStart}).
 * <li>Un elemento vacio nunca lleva cierre.
 * </ul>
 *
 * <h2>Los avisos van por metodos, no por eventos</h2>
 *
 * <p>{@link #handleStartTag} y compania son metodos protegidos que se sobrescriben. Es la forma de
 * 1997 y {@link DocumentParser} la usa para reenviar a un
 * {@link javax.swing.text.html.HTMLEditorKit.ParserCallback}.
 *
 * <h2>Errores</h2>
 *
 * <p>No se lanza nada: se avisa por {@link #handleError} y se sigue. Una pagina rota tiene que
 * mostrarse igual, aunque sea a medias. Es la diferencia entre un analizador de HTML y uno de XML.
 */
public class Parser implements DTDConstants {

    /** La DTD que gobierna el analisis. */
    protected DTD dtd = null;

    /** Si se es estricto con lo que la DTD no permite. */
    protected boolean strict = false;

    private Reader in;
    private char[] text = new char[128];
    private int textpos = 0;
    private int ch;
    private int ln = 1;
    private int pos = 0;
    private SimpleAttributeSet attributes = new SimpleAttributeSet();
    private Vector<Element> stack = new Vector<Element>();
    private boolean[] visto;
    private boolean seenHtml = false;
    private boolean seenHead = false;
    private boolean seenBody = false;
    private boolean ignoraEspacio = true;

    /** Un analizador que usa esa DTD. */
    public Parser(DTD dtd) {
        this.dtd = dtd;
        visto = new boolean[dtd.elements.size() + 64];
    }

    /** La linea que se esta leyendo, contando desde uno. */
    protected int getCurrentLine() {
        return ln;
    }

    /** Cuantos caracteres se leyeron. */
    protected int getCurrentPos() {
        return pos;
    }

    /** Una etiqueta para ese elemento, real o inventada. */
    protected TagElement makeTag(Element elem, boolean fictional) {
        return new TagElement(elem, fictional);
    }

    /** Una etiqueta real para ese elemento. */
    protected TagElement makeTag(Element elem) {
        return makeTag(elem, false);
    }

    /** Los atributos que se juntaron de la etiqueta actual. */
    protected SimpleAttributeSet getAttributes() {
        return attributes;
    }

    /** Vacia los atributos juntados. */
    protected void flushAttributes() {
        attributes.removeAttributes(attributes);
    }

    protected void handleText(char[] text) {
    }

    /** El texto del {@code <title>}; llega ademas por {@link #handleText}. */
    protected void handleTitle(char[] text) {
        handleText(text);
    }

    protected void handleComment(char[] text) {
    }

    /** El archivo se termino en la mitad de un comentario. */
    protected void handleEOFInComment() {
        error("eof.comment");
    }

    /**
     * Una etiqueta sin cierre.
     *
     * @throws ChangedCharSetException si era un {@code <meta>} que cambia la codificacion.
     */
    protected void handleEmptyTag(TagElement tag) throws ChangedCharSetException {
    }

    protected void handleStartTag(TagElement tag) {
    }

    protected void handleEndTag(TagElement tag) {
    }

    /** Un problema; ver la nota de la clase sobre por que no se lanza nada. */
    protected void handleError(int ln, String msg) {
    }

    protected void error(String err, String arg1, String arg2, String arg3) {
        handleError(ln, err + " " + arg1 + " " + arg2 + " " + arg3);
    }

    protected void error(String err, String arg1, String arg2) {
        error(err, arg1, arg2, "?");
    }

    protected void error(String err, String arg1) {
        error(err, arg1, "?", "?");
    }

    protected void error(String err) {
        error(err, "?", "?", "?");
    }

    /**
     * Abre un elemento, cerrando e insertando lo que la DTD pida.
     *
     * <p>Es donde vive la segunda de las tres reglas de la nota de la clase.
     */
    protected void startTag(TagElement tag) throws ChangedCharSetException {
        Element elem = tag.getElement();
        if (elem.isEmpty()) {
            handleEmptyTag(tag);
            return;
        }
        markFirstTime(elem);
        stack.addElement(elem);
        handleStartTag(tag);
    }

    /**
     * Cierra el elemento de arriba de la pila.
     *
     * @param omitted si el cierre no estaba escrito y lo pone el analizador.
     */
    protected void endTag(boolean omitted) {
        if (stack.isEmpty()) {
            return;
        }
        Element elem = stack.elementAt(stack.size() - 1);
        stack.removeElementAt(stack.size() - 1);
        handleEndTag(makeTag(elem, omitted));
    }

    /** Anota que un elemento aparecio por primera vez. */
    protected void markFirstTime(Element elem) {
        int i = elem.getIndex();
        if (i >= 0 && i < visto.length) {
            visto[i] = true;
        }
        if (elem == dtd.html) {
            seenHtml = true;
        } else if (elem == dtd.head) {
            seenHead = true;
        } else if (elem == dtd.body) {
            seenBody = true;
        }
    }

    /**
     * Lee una declaracion de la DTD, entre {@code <!} y {@code >}.
     *
     * @return el texto leido.
     */
    public String parseDTDMarkup() throws IOException {
        StringBuffer sb = new StringBuffer();
        while (ch != -1 && ch != '>') {
            sb.append((char) ch);
            avanzar();
        }
        if (ch == '>') {
            avanzar();
        }
        return sb.toString();
    }

    /** Lee declaraciones dentro de una seccion de marcado. */
    protected boolean parseMarkupDeclarations(StringBuffer strBuff) throws IOException {
        String s = strBuff.toString();
        return s.length() > 0;
    }

    /**
     * Analiza todo lo que venga del lector.
     *
     * <p>Al terminar cierra lo que haya quedado abierto, para que quien escucha reciba un arbol
     * cerrado aunque el documento estuviera cortado.
     */
    public synchronized void parse(Reader in) throws IOException {
        this.in = in;
        ln = 1;
        pos = 0;
        textpos = 0;
        stack.removeAllElements();
        avanzar();

        ignoraEspacio = true;
        while (ch != -1) {
            if (ch == '<') {
                leerMarca();
            } else {
                juntarTexto((char) ch);
                avanzar();
            }
        }
        volcarTexto(true);
        while (!stack.isEmpty()) {
            endTag(true);
        }
        this.in = null;
    }

    // ---- lectura de caracteres ----

    private void avanzar() throws IOException {
        ch = in.read();
        pos++;
        if (ch == '\n') {
            ln++;
        }
    }

    private void juntarTexto(char c) {
        if (textpos == text.length) {
            char[] mas = new char[text.length * 2];
            System.arraycopy(text, 0, mas, 0, text.length);
            text = mas;
        }
        text[textpos] = c;
        textpos++;
    }

    /**
     * Suelta el texto juntado, resolviendo entidades y arreglando los espacios.
     *
     * <h2>La regla de los espacios</h2>
     *
     * <p>En HTML los espacios seguidos valen por uno, y los que quedan pegados al borde de un
     * bloque no valen nada. No es un detalle: sin esta regla, un HTML escrito con sangria mostraria
     * la sangria.
     *
     * <p>Lo que hace que la regla necesite mirar hacia adelante es el borde: un espacio al final del
     * texto se descarta si lo que sigue es un bloque, y se conserva si es una etiqueta que va en la
     * linea. Por eso este metodo recibe {@code recortarFinal}, que el que leyo la marca ya sabe.
     *
     * <p>Dentro de un {@code <pre>} no se toca nada, que es justamente para lo que existe.
     */
    private void volcarTexto(boolean recortarFinal) {
        if (textpos == 0) {
            return;
        }
        char[] datos = new char[textpos];
        System.arraycopy(text, 0, datos, 0, textpos);
        textpos = 0;
        char[] resueltos = resolverEntidades(datos);
        if (!enPreformateado()) {
            resueltos = acomodarEspacios(resueltos, recortarFinal);
        }
        if (resueltos.length == 0) {
            return;
        }
        cerrarLoQueSobre(dtd.pcdata);
        abrirLoQueFalte(dtd.pcdata);
        ignoraEspacio = false;
        if (enTitulo()) {
            handleTitle(resueltos);
        } else {
            handleText(resueltos);
        }
    }

    /** Junta los espacios seguidos y saca los de los bordes que no cuentan. */
    private char[] acomodarEspacios(char[] datos, boolean recortarFinal) {
        StringBuilder sb = new StringBuilder(datos.length);
        boolean veniaEspacio = ignoraEspacio;
        for (int i = 0; i < datos.length; i++) {
            char c = datos[i];
            if (esBlanco(c)) {
                if (!veniaEspacio) {
                    sb.append(' ');
                    veniaEspacio = true;
                }
            } else {
                sb.append(c);
                veniaEspacio = false;
            }
        }
        if (recortarFinal && sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.setLength(sb.length() - 1);
        }
        char[] out = new char[sb.length()];
        sb.getChars(0, sb.length(), out, 0);
        return out;
    }

    /** Si el elemento abierto conserva los espacios tal cual. */
    private boolean enPreformateado() {
        for (int i = 0; i < stack.size(); i++) {
            HTML.Tag t = HTML.getTag(stack.elementAt(i).getName());
            if (t != null && t.isPreformatted()) {
                return true;
            }
        }
        return false;
    }

    private boolean enTitulo() {
        for (int i = 0; i < stack.size(); i++) {
            if ("title".equals(stack.elementAt(i).getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reemplaza las entidades por su texto.
     *
     * <p>Una entidad que no se conoce se deja tal como estaba. Es lo que hay que hacer: un
     * <code>&amp;</code> suelto es comun en el HTML de verdad, y convertirlo en nada perderia
     * texto del autor.
     */
    private char[] resolverEntidades(char[] datos) {
        StringBuilder sb = new StringBuilder(datos.length);
        int i = 0;
        while (i < datos.length) {
            char c = datos[i];
            if (c != '&') {
                sb.append(c);
                i++;
                continue;
            }
            int fin = -1;
            for (int j = i + 1; j < datos.length && j < i + 12; j++) {
                if (datos[j] == ';') {
                    fin = j;
                    break;
                }
                if (!Character.isLetterOrDigit(datos[j]) && datos[j] != '#') {
                    break;
                }
            }
            if (fin < 0) {
                sb.append(c);
                i++;
                continue;
            }
            String nombre = new String(datos, i + 1, fin - i - 1);
            String valor = entidad(nombre);
            if (valor == null) {
                sb.append(c);
                i++;
            } else {
                sb.append(valor);
                i = fin + 1;
            }
        }
        char[] out = new char[sb.length()];
        sb.getChars(0, sb.length(), out, 0);
        return out;
    }

    /** El texto de una entidad por nombre o por numero, o nulo si no se conoce. */
    private String entidad(String nombre) {
        if (nombre.length() == 0) {
            return null;
        }
        if (nombre.charAt(0) == '#') {
            try {
                int cod;
                if (nombre.length() > 2 && (nombre.charAt(1) == 'x' || nombre.charAt(1) == 'X')) {
                    cod = Integer.parseInt(nombre.substring(2), 16);
                } else {
                    cod = Integer.parseInt(nombre.substring(1));
                }
                return String.valueOf((char) cod);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        Entity e = dtd.getEntity(nombre);
        return (e == null) ? null : e.getString();
    }

    // ---- lectura de marcas ----

    /**
     * Lee lo que empieza con {@code <}: etiqueta, cierre, comentario o declaracion.
     *
     * <p>El nombre se lee <em>antes</em> de soltar el texto pendiente, porque el texto necesita
     * saber si lo que viene es un bloque para decidir su ultimo espacio; ver {@link #volcarTexto}.
     */
    private void leerMarca() throws IOException {
        avanzar();
        if (ch == '!') {
            avanzar();
            if (ch == '-') {
                avanzar();
                if (ch == '-') {
                    avanzar();
                    volcarTexto(false);
                    leerComentario();
                    return;
                }
            }
            volcarTexto(false);
            leerDeclaracion();
            return;
        }
        if (ch == '/') {
            avanzar();
            String nombre = leerNombre();
            volcarTexto(cortaLinea(nombre));
            leerCierre(nombre);
            return;
        }
        if (ch == -1) {
            return;
        }
        if (!Character.isLetter((char) ch)) {
            // Un `<` que no abre nada: se avisa y se descarta. Dejarlo como texto seria mas
            // amable y no es lo que hace ningun navegador -- ni el JDK.
            error("expected.tagname");
            return;
        }
        String nombre = leerNombre();
        volcarTexto(cortaLinea(nombre));
        leerApertura(nombre);
    }

    /** Si esa etiqueta corta la linea, y entonces se come el espacio que tenga al lado. */
    private static boolean cortaLinea(String nombre) {
        HTML.Tag t = HTML.getTag(nombre);
        return (t != null) && (t.isBlock() || t.breaksFlow());
    }

    private void leerComentario() throws IOException {
        StringBuilder sb = new StringBuilder();
        int guiones = 0;
        while (ch != -1) {
            if (ch == '-') {
                guiones++;
            } else if (ch == '>' && guiones >= 2) {
                avanzar();
                String s = sb.toString();
                // Los dos guiones del cierre quedaron adentro.
                if (s.length() >= 2) {
                    s = s.substring(0, s.length() - 2);
                }
                char[] datos = new char[s.length()];
                s.getChars(0, s.length(), datos, 0);
                handleComment(datos);
                return;
            } else {
                guiones = 0;
            }
            sb.append((char) ch);
            avanzar();
        }
        handleEOFInComment();
    }

    /** Una declaracion como {@code <!DOCTYPE ...>}; se lee y se descarta. */
    private void leerDeclaracion() throws IOException {
        StringBuffer sb = new StringBuffer();
        while (ch != -1 && ch != '>') {
            sb.append((char) ch);
            avanzar();
        }
        if (ch == '>') {
            avanzar();
        }
        parseMarkupDeclarations(sb);
    }

    private void leerCierre(String nombre) throws IOException {
        saltarHasta('>');
        if (nombre.length() == 0) {
            return;
        }
        if (cortaLinea(nombre)) {
            ignoraEspacio = true;
        }
        Element elem = dtd.getElement(nombre);
        cerrarHasta(elem);
    }

    /**
     * Cierra hasta ese elemento, cerrando de paso lo que quedo abierto.
     *
     * <p>Si el elemento no esta abierto no se hace nada: un cierre de mas es un error comun y
     * cerrar cualquier cosa por las dudas destruiria el arbol.
     */
    private void cerrarHasta(Element elem) {
        int i = stack.size() - 1;
        while (i >= 0 && stack.elementAt(i) != elem) {
            i--;
        }
        if (i < 0) {
            error("unmatched.endtag", elem.getName());
            return;
        }
        while (stack.size() > i + 1) {
            endTag(true);
        }
        endTag(false);
    }

    private void leerApertura(String nombre) throws IOException {
        // Los atributos van a un conjunto aparte: entre leerlos y disparar la etiqueta puede
        // haber etiquetas implicitas, y si estuvieran ya instalados se los llevaria la primera.
        SimpleAttributeSet leidos = new SimpleAttributeSet();
        leerAtributos(leidos, dtd.getElement(nombre));
        boolean cierreSolo = false;
        if (ch == '/') {
            cierreSolo = true;
            avanzar();
        }
        if (ch == '>') {
            avanzar();
        }
        if (nombre.length() == 0) {
            return;
        }
        Element elem = dtd.getElement(nombre);
        if (cortaLinea(nombre)) {
            ignoraEspacio = true;
        }
        cerrarLoQueSobre(elem);
        abrirLoQueFalte(elem);

        flushAttributes();
        attributes.addAttributes(leidos);
        TagElement tag = makeTag(elem);
        try {
            if (elem.isEmpty() || cierreSolo) {
                handleEmptyTag(tag);
            } else {
                startTag(tag);
            }
        } catch (ChangedCharSetException cse) {
            // La codificacion cambio a mitad de camino; quien nos llamo decide que hacer.
            throw new RuntimeException(cse.getMessage());
        }
    }

    /**
     * Cierra los elementos abiertos que no aceptan lo que viene.
     *
     * <p>Es la primera de las tres reglas de la nota de la clase. Solo se cierra lo que la DTD deja
     * cerrar solo: si el cierre no se puede omitir, el documento esta mal y se deja como esta.
     */
    private void cerrarLoQueSobre(Element elem) {
        while (!stack.isEmpty()) {
            Element arriba = stack.elementAt(stack.size() - 1);
            if (aceptaAdentro(arriba, elem)) {
                return;
            }
            if (!arriba.omitEnd()) {
                return;
            }
            endTag(true);
        }
    }

    /**
     * Si el padre puede llevar a ese hijo adentro.
     *
     * <p>Se pregunta si el hijo aparece <em>en algun lugar</em> del modelo, no si puede ir primero.
     * Preguntar por el primero seria lo correcto en SGML y seria inservible en HTML:
     * <code>&lt;html&gt;</code> tiene el modelo <code>(head, body, plaintext?)</code>, asi que
     * <code>&lt;body&gt;</code> no puede ir primero, y sin embargo va. El orden dentro de la
     * secuencia lo resuelve {@link #abrirLoQueFalte}, que es donde importa.
     */
    private boolean aceptaAdentro(Element padre, Element hijo) {
        if (padre.exclusions != null && hijo.getIndex() < padre.exclusions.size()
                && padre.exclusions.get(hijo.getIndex())) {
            return false;
        }
        if (padre.inclusions != null && hijo.getIndex() < padre.inclusions.size()
                && padre.inclusions.get(hijo.getIndex())) {
            return true;
        }
        ContentModel m = padre.getContent();
        if (m == null) {
            return padre.getType() == ANY;
        }
        return contiene(m, hijo);
    }

    /** Si ese elemento aparece en el modelo, a cualquier profundidad. */
    private boolean contiene(ContentModel m, Element hijo) {
        Vector<Element> v = new Vector<Element>();
        m.getElements(v);
        return v.contains(hijo);
    }

    /**
     * Abre lo que le falta a ese elemento para poder aparecer.
     *
     * <p>Es la segunda regla. Se busca un elemento intermedio cuya apertura se pueda omitir y que
     * acepte al que viene: es lo que mete el {@code <body>} de una pagina que arranca con texto.
     */
    private void abrirLoQueFalte(Element elem) {
        for (int vuelta = 0; vuelta < 8; vuelta++) {
            Element arriba = stack.isEmpty() ? null : stack.elementAt(stack.size() - 1);
            if (arriba == null) {
                if (elem == dtd.html) {
                    return;
                }
                abrirImplicito(dtd.html);
                continue;
            }
            if (aceptaAdentro(arriba, elem)) {
                saltearHasta(arriba, elem);
                return;
            }
            Element intermedio = intermedioPara(arriba, elem);
            if (intermedio == null) {
                return;
            }
            // Antes del intermedio pueden quedar salteados otros miembros de la secuencia.
            saltearHasta(arriba, intermedio);
            abrirImplicito(intermedio);
        }
    }

    /**
     * Abre y cierra los miembros de una secuencia que quedaron salteados.
     *
     * <p>El modelo de {@code html} es <code>(head, body, plaintext?)</code>. Un documento que
     * arranca con <code>&lt;body&gt;</code> se salteo el encabezado, y el encabezado existe igual:
     * se puede abrir y cerrar solo. Sin este paso, el arbol no tendria {@code head} y quien busque
     * el titulo no lo encontraria.
     *
     * <p>Solo se saltean los que se pueden abrir <em>y</em> cerrar solos. Uno que necesite
     * etiqueta escrita no se inventa: si falta, el documento esta mal y no es tarea del analizador
     * arreglarlo.
     */
    private void saltearHasta(Element padre, Element hijo) {
        ContentModel m = padre.getContent();
        if (m == null || m.type != ',') {
            return;
        }
        for (ContentModel c = (ContentModel) m.content; c != null; c = c.next) {
            if (contiene(c, hijo)) {
                return;
            }
            Element e = c.first();
            if (e == null || !e.omitStart() || !e.omitEnd() || vistoYa(e)) {
                continue;
            }
            abrirImplicito(e);
            endTag(true);
        }
    }

    private boolean vistoYa(Element e) {
        int i = e.getIndex();
        return (i >= 0 && i < visto.length && visto[i]);
    }

    /** Un elemento que pueda ir adentro del padre, aceptar al hijo, y abrirse solo. */
    private Element intermedioPara(Element padre, Element hijo) {
        ContentModel m = padre.getContent();
        if (m == null) {
            return null;
        }
        Vector<Element> candidatos = new Vector<Element>();
        m.getElements(candidatos);
        for (int i = 0; i < candidatos.size(); i++) {
            Element c = candidatos.elementAt(i);
            if (c == hijo || !c.omitStart()) {
                continue;
            }
            if (aceptaAdentro(c, hijo)) {
                return c;
            }
        }
        return null;
    }

    private void abrirImplicito(Element elem) {
        markFirstTime(elem);
        stack.addElement(elem);
        handleStartTag(makeTag(elem, true));
    }

    // ---- nombres y atributos ----

    private String leerNombre() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (ch != -1 && (Character.isLetterOrDigit((char) ch) || ch == '-' || ch == '.'
                || ch == '_' || ch == ':')) {
            sb.append(Character.toLowerCase((char) ch));
            avanzar();
        }
        return sb.toString();
    }

    private void leerAtributos(SimpleAttributeSet destino, Element elem)
            throws IOException {
        while (true) {
            saltarBlancos();
            if (ch == -1 || ch == '>' || ch == '/') {
                return;
            }
            String nombre = leerNombre();
            if (nombre.length() == 0) {
                // Un caracter que no arranca un nombre: se descarta para no quedar en un ciclo.
                avanzar();
                continue;
            }
            saltarBlancos();
            String valor = null;
            if (ch == '=') {
                avanzar();
                saltarBlancos();
                valor = leerValor();
            }
            guardarAtributo(destino, elem, nombre, valor);
        }
    }

    /**
     * Guarda un atributo con la clave que le corresponda.
     *
     * <p>Si es uno de los conocidos, la clave es la constante de {@link HTML.Attribute}; si no, el
     * nombre como cadena. Asi el resto de la biblioteca puede comparar por identidad los que le
     * importan sin perder los que no conoce.
     *
     * <p>Un atributo escrito sin valor toma el valor especial
     * {@link HTML#NULL_ATTRIBUTE_VALUE}, no nulo: guardar nulo seria indistinguible de no
     * tenerlo.
     */
    private void guardarAtributo(SimpleAttributeSet destino, Element elem, String nombre,
            String valor) {
        if (valor == null && elem != null) {
            // La forma corta de SGML: `<ul compact>` es `<ul compact="compact">`. Se reconoce
            // buscando en la DTD un atributo que tenga ese nombre entre sus valores permitidos,
            // que es exactamente para lo que existe getAttributeByValue.
            AttributeList a = elem.getAttributeByValue(nombre);
            if (a != null) {
                nombre = a.getName();
                valor = nombre;
            }
        }
        Object clave = HTML.getAttributeKey(nombre);
        if (clave == null) {
            clave = nombre;
        }
        destino.addAttribute(clave, (valor == null) ? HTML.NULL_ATTRIBUTE_VALUE : valor);
    }

    private String leerValor() throws IOException {
        StringBuilder sb = new StringBuilder();
        if (ch == '"' || ch == '\'') {
            int comilla = ch;
            avanzar();
            while (ch != -1 && ch != comilla) {
                sb.append((char) ch);
                avanzar();
            }
            if (ch == comilla) {
                avanzar();
            }
        } else {
            while (ch != -1 && !esBlanco(ch) && ch != '>') {
                sb.append((char) ch);
                avanzar();
            }
        }
        char[] datos = new char[sb.length()];
        sb.getChars(0, sb.length(), datos, 0);
        return new String(resolverEntidades(datos));
    }

    private static boolean esBlanco(int c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private void saltarBlancos() throws IOException {
        while (ch != -1 && esBlanco(ch)) {
            avanzar();
        }
    }

    private void saltarHasta(char c) throws IOException {
        while (ch != -1 && ch != c) {
            avanzar();
        }
        if (ch == c) {
            avanzar();
        }
    }
}
