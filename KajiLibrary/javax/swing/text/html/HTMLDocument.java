package javax.swing.text.html;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Un documento que guarda HTML.
 *
 * <h2>El arbol no es el HTML</h2>
 *
 * <p>Un documento de texto es parrafos con tramos de caracteres adentro. El HTML es etiquetas
 * anidadas hasta cualquier profundidad. Este documento acerca las dos formas: usa
 * {@link BlockElement} para las etiquetas que arman bloque y {@link RunElement} para los tramos, y
 * guarda en cada elemento la etiqueta que lo genero.
 *
 * <p>Lo que <em>no</em> hace es guardar las etiquetas de caracter como elementos. Una negrita no es
 * un elemento: es un atributo del tramo, con la etiqueta {@code HTML.Tag.B} como clave. Por eso el
 * arbol es mucho mas chato que el HTML, y por eso {@link HTMLWriter} tiene que reconstruir el
 * anidamiento al escribir.
 *
 * <h2>Como se llena</h2>
 *
 * <p>No se llena escribiendo texto: se llena con un {@link HTMLReader}, que es lo que
 * {@link #getReader} devuelve y lo que el analizador alimenta. El lector junta todo en una lista de
 * especificaciones y recien al final arma el arbol de una vez, que es mucho mas barato que ir
 * insertando.
 *
 * <h2>Editar el HTML directamente</h2>
 *
 * <p>Los seis metodos {@code setInnerHTML}, {@code setOuterHTML}, {@code insertAfterStart},
 * {@code insertBeforeEnd}, {@code insertBeforeStart} e {@code insertAfterEnd} permiten cambiar el
 * documento hablando en HTML en lugar de en elementos. Se nombran por lo que hacen respecto de un
 * elemento, y esa es toda la diferencia entre ellos.
 */
public class HTMLDocument extends DefaultStyledDocument {

    /** La clave con la que se guardan los comentarios que quedaron fuera del cuerpo. */
    public static final String AdditionalComments = "AdditionalComments";

    private URL base;
    private boolean preservesUnknownTags = true;
    private int tokenThreshold = Integer.MAX_VALUE;
    private HTMLEditorKit.Parser parser;
    private Hashtable<String, Element> mapa;

    /** Un documento vacio con una hoja de estilos propia. */
    public HTMLDocument() {
        this(new javax.swing.text.GapContent(BUFFER_SIZE_DEFAULT), new StyleSheet());
    }

    /** Un documento vacio con esa hoja de estilos. */
    public HTMLDocument(StyleSheet styles) {
        this(new javax.swing.text.GapContent(BUFFER_SIZE_DEFAULT), styles);
    }

    /** Un documento sobre ese contenido y esa hoja de estilos. */
    public HTMLDocument(AbstractDocument.Content c, StyleSheet styles) {
        super(c, styles);
    }

    /** Un lector que mete lo que venga a partir de esa posicion. */
    public HTMLEditorKit.ParserCallback getReader(int pos) {
        Object desc = getProperty(Document.StreamDescriptionProperty);
        if (desc instanceof URL) {
            setBase((URL) desc);
        }
        return new HTMLReader(this, pos);
    }

    /**
     * Un lector para insertar dentro de una etiqueta.
     *
     * <p>Los dos numeros dicen cuantos elementos cerrar antes y cuantos abrir despues; ver
     * {@link HTMLEditorKit#insertHTML}.
     */
    public HTMLEditorKit.ParserCallback getReader(int pos, int popDepth, int pushDepth,
            HTML.Tag insertTag) {
        return new HTMLReader(this, pos, popDepth, pushDepth, insertTag);
    }

    /** La direccion contra la que se resuelven las relativas del documento. */
    public URL getBase() {
        return base;
    }

    public void setBase(URL u) {
        base = u;
        getStyleSheet().setBase(u);
    }

    protected void insert(int offset, DefaultStyledDocument.ElementSpec[] data)
            throws BadLocationException {
        super.insert(offset, data);
    }

    protected void insertUpdate(AbstractDocument.DefaultDocumentEvent chng, AttributeSet attr) {
        if (attr == null) {
            attr = contentAttributeSet;
        }
        super.insertUpdate(chng, attr);
    }

    private static final AttributeSet contentAttributeSet = crearAtributosDeContenido();

    private static AttributeSet crearAtributosDeContenido() {
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
        return a;
    }

    protected void create(DefaultStyledDocument.ElementSpec[] data) {
        super.create(data);
    }

    public void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace) {
        super.setParagraphAttributes(offset, length, s, replace);
    }

    /** La hoja de estilos: es el contexto de atributos del documento. */
    public StyleSheet getStyleSheet() {
        return (StyleSheet) getAttributeContext();
    }

    /**
     * Recorre todos los elementos con esa etiqueta.
     *
     * <p>Es la forma de encontrar, por ejemplo, todos los enlaces de una pagina sin bajar a mano
     * por el arbol.
     */
    public Iterator getIterator(HTML.Tag t) {
        if (t.isBlock()) {
            return new BlockIterator(this, t);
        }
        return new LeafIterator(this, t);
    }

    protected Element createLeafElement(Element parent, AttributeSet a, int p0, int p1) {
        return new RunElement(this, parent, a, p0, p1);
    }

    protected Element createBranchElement(Element parent, AttributeSet a) {
        return new BlockElement(this, parent, a);
    }

    protected AbstractDocument.AbstractElement createDefaultRoot() {
        writeLock();
        MutableAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.HTML);
        BlockElement raiz = new BlockElement(this, null, a.copyAttributes());
        a.removeAttributes(a);

        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.BODY);
        BlockElement cuerpo = new BlockElement(this, raiz, a.copyAttributes());
        a.removeAttributes(a);

        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.P);
        BlockElement parrafo = new BlockElement(this, cuerpo, a.copyAttributes());
        a.removeAttributes(a);

        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
        RunElement contenido = new RunElement(this, parrafo, a, 0, 1);
        Element[] hijos = new Element[1];
        hijos[0] = contenido;
        parrafo.replace(0, 0, hijos);
        hijos[0] = parrafo;
        cuerpo.replace(0, 0, hijos);
        hijos[0] = cuerpo;
        raiz.replace(0, 0, hijos);
        writeUnlock();
        return raiz;
    }

    /**
     * Cuantos elementos se juntan antes de tocar el documento.
     *
     * <p>Con un numero chico el documento se ve armandose de a poco; con uno grande aparece de una
     * vez y tarda menos en total. Es el compromiso entre parecer rapido y serlo.
     */
    public void setTokenThreshold(int n) {
        putProperty(TokenThreshold, Integer.valueOf(n));
        tokenThreshold = n;
    }

    public int getTokenThreshold() {
        return tokenThreshold;
    }

    static final String TokenThreshold = "token threshold";

    /**
     * Si las etiquetas que no se conocen se guardan igual.
     *
     * <p>Guardarlas permite volver a escribir el documento tal como entro, aunque no se sepa
     * dibujarlas. Es lo que se quiere casi siempre: tirar lo que no se entiende pierde datos del
     * usuario.
     */
    public void setPreservesUnknownTags(boolean preservesTags) {
        preservesUnknownTags = preservesTags;
    }

    public boolean getPreservesUnknownTags() {
        return preservesUnknownTags;
    }

    /** Carga el documento del enlace en el marco que indique el evento. */
    public void processHTMLFrameHyperlinkEvent(HTMLFrameHyperlinkEvent e) {
    }

    /** El analizador que usan los metodos que insertan HTML. */
    public void setParser(HTMLEditorKit.Parser parser) {
        this.parser = parser;
        putProperty("__PARSER__", null);
    }

    public HTMLEditorKit.Parser getParser() {
        Object p = getProperty("__PARSER__");
        if (p instanceof HTMLEditorKit.Parser) {
            return (HTMLEditorKit.Parser) p;
        }
        return parser;
    }

    /**
     * Reemplaza el contenido de un elemento por ese HTML.
     *
     * @throws IllegalStateException si no hay analizador.
     */
    public void setInnerHTML(Element elem, String htmlText) throws BadLocationException,
            IOException {
        verificarAnalizador();
        if (elem == null || htmlText == null) {
            throw new IllegalArgumentException("null parameter");
        }
        if (elem.isLeaf()) {
            throw new IllegalArgumentException(
                    "Can not set inner HTML of a leaf");
        }
        int inicio = elem.getStartOffset();
        int fin = elem.getEndOffset();
        insertar(inicio, htmlText, 0, 0, null);
        borrarTramo(inicio + largoInsertado, fin - inicio);
    }

    /** Reemplaza el elemento entero, incluidas sus etiquetas. */
    public void setOuterHTML(Element elem, String htmlText) throws BadLocationException,
            IOException {
        verificarAnalizador();
        if (elem == null || htmlText == null) {
            throw new IllegalArgumentException("null parameter");
        }
        int inicio = elem.getStartOffset();
        int fin = elem.getEndOffset();
        insertar(inicio, htmlText, 0, 0, null);
        borrarTramo(inicio + largoInsertado, fin - inicio);
    }

    /** Inserta justo despues de la etiqueta de apertura. */
    public void insertAfterStart(Element elem, String htmlText) throws BadLocationException,
            IOException {
        verificarAnalizador();
        if (elem == null || htmlText == null) {
            throw new IllegalArgumentException("null parameter");
        }
        if (elem.isLeaf()) {
            throw new IllegalArgumentException(
                    "Can not insert HTML after start of a leaf");
        }
        insertar(elem.getStartOffset(), htmlText, 0, 0, null);
    }

    /** Inserta justo antes de la etiqueta de cierre. */
    public void insertBeforeEnd(Element elem, String htmlText) throws BadLocationException,
            IOException {
        verificarAnalizador();
        if (elem == null || htmlText == null) {
            throw new IllegalArgumentException("null parameter");
        }
        if (elem.isLeaf()) {
            throw new IllegalArgumentException(
                    "Can not set inner HTML before end of leaf");
        }
        int fin = elem.getEndOffset();
        if (fin > getLength()) {
            fin = getLength();
        }
        insertar(fin, htmlText, 0, 0, null);
    }

    /** Inserta antes de la etiqueta de apertura, o sea afuera del elemento. */
    public void insertBeforeStart(Element elem, String htmlText) throws BadLocationException,
            IOException {
        verificarAnalizador();
        if (elem == null || htmlText == null) {
            throw new IllegalArgumentException("null parameter");
        }
        insertar(elem.getStartOffset(), htmlText, 0, 0, null);
    }

    /** Inserta despues de la etiqueta de cierre. */
    public void insertAfterEnd(Element elem, String htmlText) throws BadLocationException,
            IOException {
        verificarAnalizador();
        if (elem == null || htmlText == null) {
            throw new IllegalArgumentException("null parameter");
        }
        int fin = elem.getEndOffset();
        if (fin > getLength()) {
            fin = getLength();
        }
        insertar(fin, htmlText, 0, 0, null);
    }

    private int largoInsertado;

    private void verificarAnalizador() {
        if (getParser() == null) {
            throw new IllegalStateException("No HTMLEditorKit.Parser");
        }
    }

    private void insertar(int offset, String html, int popDepth, int pushDepth, HTML.Tag insertTag)
            throws BadLocationException, IOException {
        int antes = getLength();
        HTMLEditorKit.ParserCallback r = getReader(offset, popDepth, pushDepth, insertTag);
        getParser().parse(new java.io.StringReader(html), r, true);
        r.flush();
        largoInsertado = getLength() - antes;
    }

    private void borrarTramo(int offset, int largo) throws BadLocationException {
        if (largo > 0 && offset < getLength()) {
            remove(offset, Math.min(largo, getLength() - offset));
        }
    }

    /**
     * El elemento cuyo atributo <code>id</code> es ese.
     *
     * <p>Es lo que hace que un enlace a <code>#seccion</code> pueda encontrar su destino.
     */
    public Element getElement(String id) {
        if (id == null) {
            return null;
        }
        return getElement(getDefaultRootElement(), HTML.Attribute.ID, id, true);
    }

    /** El primer elemento debajo de ese cuyo atributo tenga ese valor. */
    public Element getElement(Element e, Object attribute, Object value) {
        return getElement(e, attribute, value, true);
    }

    private Element getElement(Element e, Object attribute, Object value, boolean searchLeafs) {
        AttributeSet attr = e.getAttributes();
        if (attr != null && attr.isDefined(attribute)) {
            if (value.equals(attr.getAttribute(attribute))) {
                return e;
            }
        }
        if (!e.isLeaf()) {
            int maxCounter = e.getElementCount();
            for (int counter = 0; counter < maxCounter; counter++) {
                Element child = e.getElement(counter);
                if (searchLeafs || !child.isLeaf()) {
                    Element retValue = getElement(child, attribute, value, searchLeafs);
                    if (retValue != null) {
                        return retValue;
                    }
                }
            }
        }
        return null;
    }

    protected void fireChangedUpdate(DocumentEvent e) {
        super.fireChangedUpdate(e);
    }

    protected void fireUndoableEditUpdate(UndoableEditEvent e) {
        super.fireUndoableEditUpdate(e);
    }

    /**
     * Recorre los elementos que tienen una etiqueta.
     *
     * <p>Es abstracta y no una interfaz porque tiene que poder crecer sin romper a quien la use;
     * ver {@link HTMLDocument#getIterator}.
     */
    public abstract static class Iterator {

        protected Iterator() {
        }

        /** Los atributos del elemento actual. */
        public abstract AttributeSet getAttributes();

        public abstract int getStartOffset();

        public abstract int getEndOffset();

        /** Avanza al siguiente. */
        public abstract void next();

        /** Si todavia hay elemento. */
        public abstract boolean isValid();

        public abstract HTML.Tag getTag();
    }

    /**
     * Recorre las etiquetas de caracter, que no son elementos sino atributos.
     *
     * <p>Una negrita o un enlace viven como atributo de un tramo; ver la nota de la clase que la
     * contiene. Por eso este recorrido mira los atributos de cada hoja y no los elementos.
     */
    static class LeafIterator extends Iterator {

        private final HTML.Tag tag;
        private final javax.swing.text.ElementIterator pos;
        private AttributeSet actual;
        private int inicio;
        private int fin;

        LeafIterator(HTMLDocument doc, HTML.Tag t) {
            tag = t;
            pos = new javax.swing.text.ElementIterator(doc);
            next();
        }

        public AttributeSet getAttributes() {
            return actual;
        }

        public int getStartOffset() {
            return inicio;
        }

        public int getEndOffset() {
            return fin;
        }

        public HTML.Tag getTag() {
            return tag;
        }

        public boolean isValid() {
            return actual != null;
        }

        /**
         * Busca la proxima hoja con la etiqueta y junta las contiguas.
         *
         * <p>Juntarlas importa: un enlace partido en dos tramos porque uno tiene negrita adentro
         * sigue siendo un solo enlace, y quien recorre los enlaces espera verlo una vez.
         */
        public void next() {
            actual = null;
            Element e;
            while ((e = pos.next()) != null) {
                if (!e.isLeaf()) {
                    continue;
                }
                AttributeSet a = e.getAttributes();
                Object v = a.getAttribute(tag);
                if (v instanceof AttributeSet) {
                    actual = (AttributeSet) v;
                    inicio = e.getStartOffset();
                    fin = e.getEndOffset();
                    // Se estira mientras el atributo sea el mismo objeto.
                    Element sig;
                    while ((sig = pos.next()) != null) {
                        if (sig.isLeaf() && sig.getAttributes().getAttribute(tag) == v) {
                            fin = sig.getEndOffset();
                        } else {
                            pos.previous();
                            break;
                        }
                    }
                    return;
                }
            }
        }
    }

    /** Recorre los elementos que arman bloque y tienen esa etiqueta. */
    static class BlockIterator extends Iterator {

        private final HTML.Tag tag;
        private final javax.swing.text.ElementIterator pos;
        private Element actual;

        BlockIterator(HTMLDocument doc, HTML.Tag t) {
            tag = t;
            pos = new javax.swing.text.ElementIterator(doc);
            next();
        }

        public AttributeSet getAttributes() {
            return (actual == null) ? null : actual.getAttributes();
        }

        public int getStartOffset() {
            return (actual == null) ? -1 : actual.getStartOffset();
        }

        public int getEndOffset() {
            return (actual == null) ? -1 : actual.getEndOffset();
        }

        public HTML.Tag getTag() {
            return tag;
        }

        public boolean isValid() {
            return actual != null;
        }

        public void next() {
            actual = null;
            Element e;
            while ((e = pos.next()) != null) {
                if (e.getAttributes().getAttribute(StyleConstants.NameAttribute) == tag) {
                    actual = e;
                    return;
                }
            }
        }
    }

    /**
     * Un elemento con hijos que sabe de que etiqueta salio.
     *
     * <p>En el JDK es una clase interna; aca es estatica y recibe el documento, que es la misma
     * firma del archivo compilado. Ver la nota de {@link javax.swing.text.TableView.TableRow}.
     */
    public static class BlockElement extends AbstractDocument.BranchElement {

        /** Un bloque de ese documento, colgado de ese padre. */
        public BlockElement(HTMLDocument documento, Element parent, AttributeSet a) {
            super(documento, parent, a);
        }

        /** El nombre es el de la etiqueta, no el del tipo de elemento. */
        public String getName() {
            Object o = getAttribute(StyleConstants.NameAttribute);
            if (o != null) {
                return o.toString();
            }
            return super.getName();
        }

        /**
         * De donde hereda los atributos que no tiene.
         *
         * <p>De la hoja de estilos, no del elemento de arriba. Es la diferencia entre HTML y un
         * documento comun: quien decide como se ve un parrafo es la regla de CSS que le
         * corresponde, y esa regla depende de todo el camino desde la raiz.
         */
        public AttributeSet getResolveParent() {
            return null;
        }
    }

    /** Un tramo de texto que sabe de que etiqueta salio. */
    public static class RunElement extends AbstractDocument.LeafElement {

        /** Un tramo de ese documento, colgado de ese padre. */
        public RunElement(HTMLDocument documento, Element parent, AttributeSet a, int offs0,
                int offs1) {
            super(documento, parent, a, offs0, offs1);
        }

        public String getName() {
            Object o = getAttribute(StyleConstants.NameAttribute);
            if (o != null) {
                return o.toString();
            }
            return super.getName();
        }

        /** Igual que en {@link BlockElement}. */
        public AttributeSet getResolveParent() {
            return null;
        }
    }

    /**
     * Convierte lo que encuentra el analizador en elementos del documento.
     *
     * <h2>Junta y despues arma</h2>
     *
     * <p>No inserta a medida que lee: guarda especificaciones en {@link #parseBuffer} y arma el
     * arbol cuando junta bastantes o cuando termina. Insertar de a una etiqueta obligaria a
     * rehacer el arbol en cada paso.
     *
     * <h2>Una accion por etiqueta</h2>
     *
     * <p>Cada etiqueta tiene una {@link TagAction} registrada. Es una tabla y no una cadena de
     * comparaciones: agregar una etiqueta es registrar una accion, y quien herede puede cambiar el
     * tratamiento de una sola sin tocar el resto.
     */
    public static class HTMLReader extends HTMLEditorKit.ParserCallback {

        /** Lo que se junto hasta ahora. */
        protected Vector<DefaultStyledDocument.ElementSpec> parseBuffer =
                new Vector<DefaultStyledDocument.ElementSpec>();

        /** Los atributos de caracter que valen ahora. */
        protected MutableAttributeSet charAttr = new SimpleAttributeSet();

        final HTMLDocument documento;
        private final int offset;
        private int popDepth;
        private int pushDepth;
        private HTML.Tag insertTag;
        private Hashtable<HTML.Tag, TagAction> tagMap = new Hashtable<HTML.Tag, TagAction>();
        private Stack<AttributeSet> charAttrStack = new Stack<AttributeSet>();
        private MutableAttributeSet atributosBloque = new SimpleAttributeSet();
        private boolean insertoAlgo = false;

        /** Un lector que mete a partir de esa posicion. */
        public HTMLReader(HTMLDocument documento, int offset) {
            this(documento, offset, 0, 0, null);
        }

        /** Un lector para insertar dentro de una etiqueta. */
        public HTMLReader(HTMLDocument documento, int offset, int popDepth, int pushDepth,
                HTML.Tag insertTag) {
            this.documento = documento;
            this.offset = offset;
            this.popDepth = popDepth;
            this.pushDepth = pushDepth;
            this.insertTag = insertTag;
            registrarAcciones();
        }

        private void registrarAcciones() {
            TagAction bloque = new BlockAction(this);
            TagAction parrafo = new ParagraphAction(this);
            TagAction caracter = new CharacterAction(this);
            TagAction especial = new SpecialAction(this);
            TagAction oculta = new HiddenAction(this);
            TagAction pre = new PreAction(this);
            TagAction formulario = new FormAction(this);

            registerTag(HTML.Tag.HTML, bloque);
            registerTag(HTML.Tag.BODY, bloque);
            registerTag(HTML.Tag.DIV, bloque);
            registerTag(HTML.Tag.CENTER, bloque);
            registerTag(HTML.Tag.BLOCKQUOTE, bloque);
            registerTag(HTML.Tag.UL, bloque);
            registerTag(HTML.Tag.OL, bloque);
            registerTag(HTML.Tag.DIR, bloque);
            registerTag(HTML.Tag.MENU, bloque);
            registerTag(HTML.Tag.LI, bloque);
            registerTag(HTML.Tag.DL, bloque);
            registerTag(HTML.Tag.DD, bloque);
            registerTag(HTML.Tag.TABLE, bloque);
            registerTag(HTML.Tag.TR, bloque);
            registerTag(HTML.Tag.TD, bloque);
            registerTag(HTML.Tag.TH, bloque);
            registerTag(HTML.Tag.CAPTION, bloque);
            registerTag(HTML.Tag.NOFRAMES, bloque);

            registerTag(HTML.Tag.P, parrafo);
            registerTag(HTML.Tag.IMPLIED, parrafo);
            registerTag(HTML.Tag.DT, parrafo);
            registerTag(HTML.Tag.H1, parrafo);
            registerTag(HTML.Tag.H2, parrafo);
            registerTag(HTML.Tag.H3, parrafo);
            registerTag(HTML.Tag.H4, parrafo);
            registerTag(HTML.Tag.H5, parrafo);
            registerTag(HTML.Tag.H6, parrafo);

            registerTag(HTML.Tag.PRE, pre);

            registerTag(HTML.Tag.B, caracter);
            registerTag(HTML.Tag.I, caracter);
            registerTag(HTML.Tag.U, caracter);
            registerTag(HTML.Tag.S, caracter);
            registerTag(HTML.Tag.STRIKE, caracter);
            registerTag(HTML.Tag.TT, caracter);
            registerTag(HTML.Tag.CODE, caracter);
            registerTag(HTML.Tag.KBD, caracter);
            registerTag(HTML.Tag.SAMP, caracter);
            registerTag(HTML.Tag.VAR, caracter);
            registerTag(HTML.Tag.CITE, caracter);
            registerTag(HTML.Tag.DFN, caracter);
            registerTag(HTML.Tag.EM, caracter);
            registerTag(HTML.Tag.STRONG, caracter);
            registerTag(HTML.Tag.BIG, caracter);
            registerTag(HTML.Tag.SMALL, caracter);
            registerTag(HTML.Tag.SUB, caracter);
            registerTag(HTML.Tag.SUP, caracter);
            registerTag(HTML.Tag.FONT, caracter);
            registerTag(HTML.Tag.SPAN, caracter);
            registerTag(HTML.Tag.A, caracter);
            registerTag(HTML.Tag.ADDRESS, caracter);

            registerTag(HTML.Tag.IMG, especial);
            registerTag(HTML.Tag.BR, especial);
            registerTag(HTML.Tag.HR, especial);
            registerTag(HTML.Tag.OBJECT, especial);
            registerTag(HTML.Tag.APPLET, especial);
            registerTag(HTML.Tag.PARAM, especial);

            registerTag(HTML.Tag.INPUT, formulario);
            registerTag(HTML.Tag.SELECT, formulario);
            registerTag(HTML.Tag.OPTION, formulario);
            registerTag(HTML.Tag.TEXTAREA, formulario);
            registerTag(HTML.Tag.FORM, formulario);

            registerTag(HTML.Tag.HEAD, oculta);
            registerTag(HTML.Tag.TITLE, oculta);
            registerTag(HTML.Tag.META, oculta);
            registerTag(HTML.Tag.LINK, oculta);
            registerTag(HTML.Tag.STYLE, oculta);
            registerTag(HTML.Tag.SCRIPT, oculta);
            registerTag(HTML.Tag.AREA, oculta);
            registerTag(HTML.Tag.MAP, oculta);
            registerTag(HTML.Tag.BASE, oculta);
            registerTag(HTML.Tag.BASEFONT, oculta);
            registerTag(HTML.Tag.ISINDEX, new IsindexAction(this));
            registerTag(HTML.Tag.FRAMESET, oculta);
            registerTag(HTML.Tag.FRAME, oculta);
        }

        /** Vuelca lo que quedo juntado al documento. */
        public void flush() throws BadLocationException {
            if (parseBuffer.size() == 0) {
                return;
            }
            DefaultStyledDocument.ElementSpec[] spec =
                    new DefaultStyledDocument.ElementSpec[parseBuffer.size()];
            parseBuffer.copyInto(spec);
            parseBuffer.removeAllElements();
            if (documento.getLength() == 0 && offset == 0 && !insertoAlgo) {
                documento.create(spec);
            } else {
                documento.insert(offset, spec);
            }
            insertoAlgo = true;
        }

        public void handleText(char[] data, int pos) {
            if (data.length == 0) {
                return;
            }
            addContent(data, 0, data.length);
        }

        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            TagAction action = tagMap.get(t);
            if (action != null) {
                action.start(t, a);
            } else if (documento.getPreservesUnknownTags()) {
                // Una etiqueta que no se conoce se guarda como bloque; ver
                // HTMLDocument.setPreservesUnknownTags.
                blockOpen(t, a);
            }
        }

        public void handleComment(char[] data, int pos) {
            SimpleAttributeSet a = new SimpleAttributeSet();
            a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.COMMENT);
            a.addAttribute(HTML.Attribute.COMMENT, new String(data));
            addSpecialElement(HTML.Tag.COMMENT, a);
        }

        public void handleEndTag(HTML.Tag t, int pos) {
            TagAction action = tagMap.get(t);
            if (action != null) {
                action.end(t);
            } else if (documento.getPreservesUnknownTags()) {
                blockClose(t);
            }
        }

        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            TagAction action = tagMap.get(t);
            if (action != null) {
                action.start(t, a);
                action.end(t);
            } else if (documento.getPreservesUnknownTags()) {
                addSpecialElement(t, a);
            }
        }

        public void handleEndOfLineString(String eol) {
            if (eol != null) {
                documento.putProperty(javax.swing.text.DefaultEditorKit.EndOfLineStringProperty,
                        eol);
            }
        }

        /** Asocia una accion a una etiqueta; ver la nota de la clase. */
        protected void registerTag(HTML.Tag t, TagAction a) {
            tagMap.put(t, a);
        }

        /**
         * Guarda los atributos de caracter que valen ahora.
         *
         * <p>Van a una pila porque las etiquetas de caracter se anidan: al cerrar una hay que
         * volver exactamente a lo que habia antes de abrirla, no a nada.
         */
        protected void pushCharacterStyle() {
            charAttrStack.push(charAttr.copyAttributes());
        }

        protected void popCharacterStyle() {
            if (!charAttrStack.isEmpty()) {
                charAttr = new SimpleAttributeSet(charAttrStack.pop());
            }
        }

        /** El contenido de un {@code <textarea>}: va al modelo del control, no al documento. */
        protected void textAreaContent(char[] data) {
        }

        /** Texto dentro de un {@code <pre>}: los saltos de linea cuentan. */
        protected void preContent(char[] data) {
            int inicio = 0;
            for (int i = 0; i < data.length; i++) {
                if (data[i] == '\n') {
                    addContent(data, inicio, i - inicio + 1);
                    blockClose(HTML.Tag.IMPLIED);
                    blockOpen(HTML.Tag.IMPLIED, new SimpleAttributeSet());
                    inicio = i + 1;
                }
            }
            if (inicio < data.length) {
                addContent(data, inicio, data.length - inicio);
            }
        }

        /** Abre un bloque. */
        protected void blockOpen(HTML.Tag t, MutableAttributeSet attr) {
            attr.addAttribute(StyleConstants.NameAttribute, t);
            parseBuffer.addElement(new DefaultStyledDocument.ElementSpec(
                    attr.copyAttributes(), DefaultStyledDocument.ElementSpec.StartTagType));
        }

        protected void blockClose(HTML.Tag t) {
            parseBuffer.addElement(new DefaultStyledDocument.ElementSpec(
                    null, DefaultStyledDocument.ElementSpec.EndTagType));
        }

        protected void addContent(char[] data, int offs, int length) {
            addContent(data, offs, length, true);
        }

        /**
         * Agrega texto con los atributos de caracter que valen ahora.
         *
         * <p>Si no hay ningun bloque abierto se abre un parrafo implicito: el texto suelto tiene
         * que vivir adentro de un parrafo, porque el documento no admite texto colgando de la raiz.
         */
        protected void addContent(char[] data, int offs, int length, boolean generateImpliedPIfNecessary) {
            MutableAttributeSet a = new SimpleAttributeSet(charAttr);
            a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
            DefaultStyledDocument.ElementSpec spec = new DefaultStyledDocument.ElementSpec(
                    a.copyAttributes(), DefaultStyledDocument.ElementSpec.ContentType, data, offs,
                    length);
            parseBuffer.addElement(spec);
            if (parseBuffer.size() > documento.getTokenThreshold()) {
                try {
                    flush();
                } catch (BadLocationException ble) {
                    // El documento cambio debajo: se sigue juntando.
                }
            }
        }

        /**
         * Agrega un elemento que ocupa lugar pero no tiene texto.
         *
         * <p>Una imagen, un salto de linea, un comentario. Se guardan como un caracter invisible
         * con atributos: el documento cuenta en caracteres y algo que ocupa un lugar tiene que
         * ocupar uno.
         */
        protected void addSpecialElement(HTML.Tag t, MutableAttributeSet a) {
            if (t != HTML.Tag.FRAME && !insertoAlgo) {
                // Ya se puede.
                insertoAlgo = false;
            }
            a.addAttribute(StyleConstants.NameAttribute, t);
            char[] uno = {' '};
            parseBuffer.addElement(new DefaultStyledDocument.ElementSpec(a.copyAttributes(),
                    DefaultStyledDocument.ElementSpec.ContentType, uno, 0, 1));
        }

        /**
         * Que hacer con una etiqueta.
         *
         * <p>La de base no hace nada. Sirve para las etiquetas que se quieren ignorar sin que el
         * lector tenga que preguntar si hay accion registrada.
         */
        public static class TagAction {

            final HTMLReader lector;

            /** Una accion para ese lector. */
            public TagAction(HTMLReader lector) {
                this.lector = lector;
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
            }

            public void end(HTML.Tag t) {
            }
        }

        /** Una etiqueta que arma bloque. */
        public static class BlockAction extends TagAction {

            public BlockAction(HTMLReader lector) {
                super(lector);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                lector.blockOpen(t, a);
            }

            public void end(HTML.Tag t) {
                lector.blockClose(t);
            }
        }

        /**
         * Una etiqueta de caracter: no arma elemento, cambia los atributos del texto.
         *
         * <p>Ver la nota de {@link HTMLDocument}: es lo que hace que el arbol sea mucho mas chato
         * que el HTML.
         */
        public static class CharacterAction extends TagAction {

            public CharacterAction(HTMLReader lector) {
                super(lector);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                lector.pushCharacterStyle();
                a.addAttribute(t, a.copyAttributes());
                lector.charAttr.addAttributes(a);
            }

            public void end(HTML.Tag t) {
                lector.popCharacterStyle();
            }
        }

        /** Un parrafo: un bloque que ademas cierra el implicito que hubiera abierto. */
        public static class ParagraphAction extends BlockAction {

            public ParagraphAction(HTMLReader lector) {
                super(lector);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                super.start(t, a);
            }

            public void end(HTML.Tag t) {
                super.end(t);
            }
        }

        /** Un {@code <pre>}: como un bloque, pero el texto de adentro respeta los saltos. */
        public static class PreAction extends BlockAction {

            public PreAction(HTMLReader lector) {
                super(lector);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                a.addAttribute(CSS.Attribute.WHITE_SPACE, "pre");
                super.start(t, a);
                lector.blockOpen(HTML.Tag.IMPLIED, new SimpleAttributeSet());
            }

            public void end(HTML.Tag t) {
                lector.blockClose(HTML.Tag.IMPLIED);
                super.end(t);
            }
        }

        /** Una etiqueta que ocupa un lugar sin texto: imagen, salto, linea. */
        public static class SpecialAction extends TagAction {

            public SpecialAction(HTMLReader lector) {
                super(lector);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                lector.addSpecialElement(t, a);
            }
        }

        /** Un control de formulario. */
        public static class FormAction extends SpecialAction {

            public FormAction(HTMLReader lector) {
                super(lector);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                if (t == HTML.Tag.FORM) {
                    lector.blockOpen(t, a);
                    return;
                }
                super.start(t, a);
            }

            public void end(HTML.Tag t) {
                if (t == HTML.Tag.FORM) {
                    lector.blockClose(t);
                }
            }
        }

        /**
         * Una etiqueta que no se ve pero se guarda.
         *
         * <p>El encabezado, un {@code <script>}, un {@code <meta>}. Se guardan para poder volver a
         * escribir el documento igual; ver {@link HTMLDocument#setPreservesUnknownTags}.
         */
        public static class HiddenAction extends TagAction {

            public HiddenAction(HTMLReader lector) {
                super(lector);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                lector.addSpecialElement(t, a);
            }

            public void end(HTML.Tag t) {
            }
        }

        /** Un {@code <isindex>}: se muestra como un campo de busqueda. */
        public static class IsindexAction extends TagAction {

            public IsindexAction(HTMLReader lector) {
                super(lector);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                lector.blockOpen(HTML.Tag.IMPLIED, new SimpleAttributeSet());
                lector.addSpecialElement(t, a);
                lector.blockClose(HTML.Tag.IMPLIED);
            }
        }
    }
}
