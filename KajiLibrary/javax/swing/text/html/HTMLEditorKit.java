package javax.swing.text.html;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * El juego de edicion para HTML.
 *
 * <h2>Que junta</h2>
 *
 * <p>Un analizador que convierte texto HTML en un {@link HTMLDocument}, una fabrica de vistas que
 * sabe dibujar cada etiqueta, una hoja de estilos con las reglas por omision, y las acciones que
 * editan.
 *
 * <h2>El analizador no viene de fabrica</h2>
 *
 * <p>{@link #getParser} lo busca en {@code javax.swing.text.html.parser.ParserDelegator}, por
 * reflexion y no por una referencia directa. Es a proposito: el juego de edicion no depende de que
 * exista un analizador concreto, y quien quiera otro solo tiene que darle un
 * {@link HTMLDocument#setParser}.
 *
 * <h2>La hoja de estilos se comparte</h2>
 *
 * <p>Todos los juegos de edicion que no pidan la suya usan la misma. Es lo que permite que una
 * regla puesta una vez valga para todos los paneles del programa; y es tambien por lo que
 * {@link #setStyleSheet} conviene usarlo con cuidado, porque cambia la de todos.
 */
public class HTMLEditorKit extends StyledEditorKit implements Accessible {

    /** El nombre del recurso con la hoja de estilos por omision. */
    public static final String DEFAULT_CSS = "default.css";

    public static final String BOLD_ACTION = "html-bold-action";
    public static final String ITALIC_ACTION = "html-italic-action";
    public static final String PARA_INDENT_LEFT = "html-para-indent-left";
    public static final String PARA_INDENT_RIGHT = "html-para-indent-right";
    public static final String FONT_CHANGE_BIGGER = "html-font-bigger";
    public static final String FONT_CHANGE_SMALLER = "html-font-smaller";
    public static final String COLOR_ACTION = "html-color-action";
    public static final String LOGICAL_STYLE_ACTION = "html-logical-style-action";
    public static final String IMG_ALIGN_TOP = "html-image-align-top";
    public static final String IMG_ALIGN_MIDDLE = "html-image-align-middle";
    public static final String IMG_ALIGN_BOTTOM = "html-image-align-bottom";
    public static final String IMG_BORDER = "html-image-border";

    private static final Cursor MoveCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor DefaultCursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);

    private static final ViewFactory defaultFactory = new HTMLFactory();
    private static StyleSheet defaultStyles = null;

    private Cursor defaultCursor = DefaultCursor;
    private Cursor linkCursor = MoveCursor;
    private boolean isAutoFormSubmission = true;
    private MutableAttributeSet input;
    private LinkController linkHandler = new LinkController();
    private AccessibleContext accessibleContext;

    /** Un juego de edicion listo para usar. */
    public HTMLEditorKit() {
    }

    /** Siempre {@code text/html}. */
    public String getContentType() {
        return "text/html";
    }

    public ViewFactory getViewFactory() {
        return defaultFactory;
    }

    /** Un documento vacio, ya atado a la hoja de estilos de este juego. */
    public Document createDefaultDocument() {
        StyleSheet styles = getStyleSheet();
        StyleSheet ss = new StyleSheet();
        ss.addStyleSheet(styles);
        HTMLDocument doc = new HTMLDocument(ss);
        doc.setParser(getParser());
        doc.setAsynchronousLoadPriority(4);
        doc.setTokenThreshold(100);
        return doc;
    }

    /**
     * Lee HTML y lo mete en el documento en esa posicion.
     *
     * @throws IOException si no hay analizador o falla la lectura.
     */
    public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException {
        if (doc instanceof HTMLDocument) {
            HTMLDocument hdoc = (HTMLDocument) doc;
            if (pos > doc.getLength()) {
                throw new BadLocationException("Invalid location", pos);
            }
            Parser p = getParser();
            if (p == null) {
                throw new IOException("Can't load parser");
            }
            ParserCallback receiver = hdoc.getReader(pos);
            Boolean ignoreCharset = (Boolean) doc.getProperty("IgnoreCharsetDirective");
            p.parse(in, receiver, (ignoreCharset == null) ? false : ignoreCharset.booleanValue());
            receiver.flush();
        } else {
            super.read(in, doc, pos);
        }
    }

    /**
     * Mete un fragmento de HTML dentro de una etiqueta del documento.
     *
     * <p>Los dos numeros {@code popDepth} y {@code pushDepth} son los que hacen que el fragmento
     * caiga en el lugar correcto del arbol: cuantos elementos hay que cerrar antes y cuantos abrir
     * despues. Sin ellos, insertar un <code>&lt;li&gt;</code> lo dejaria fuera de su lista.
     */
    public void insertHTML(HTMLDocument doc, int offset, String html, int popDepth, int pushDepth,
            HTML.Tag insertTag) throws BadLocationException, IOException {
        Parser p = getParser();
        if (p == null) {
            throw new IOException("Can't load parser");
        }
        if (offset > doc.getLength()) {
            throw new BadLocationException("Invalid location", offset);
        }
        ParserCallback receiver = doc.getReader(offset, popDepth, pushDepth, insertTag);
        Boolean ignoreCharset = (Boolean) doc.getProperty("IgnoreCharsetDirective");
        p.parse(new java.io.StringReader(html), receiver,
                (ignoreCharset == null) ? false : ignoreCharset.booleanValue());
        receiver.flush();
    }

    /** Escribe el documento como HTML. */
    public void write(Writer out, Document doc, int pos, int len) throws IOException,
            BadLocationException {
        if (doc instanceof HTMLDocument) {
            HTMLWriter w = new HTMLWriter(out, (HTMLDocument) doc, pos, len);
            w.write();
        } else if (doc instanceof javax.swing.text.StyledDocument) {
            MinimalHTMLWriter w = new MinimalHTMLWriter(out,
                    (javax.swing.text.StyledDocument) doc, pos, len);
            w.write();
        } else {
            super.write(out, doc, pos, len);
        }
    }

    /** Se engancha al panel y le pone el vigilante de enlaces. */
    public void install(JEditorPane c) {
        c.addMouseListener(linkHandler);
        c.addMouseMotionListener(linkHandler);
        super.install(c);
    }

    public void deinstall(JEditorPane c) {
        c.removeMouseListener(linkHandler);
        c.removeMouseMotionListener(linkHandler);
        super.deinstall(c);
    }

    /** La hoja de estilos de este juego; ver la nota de la clase. */
    public void setStyleSheet(StyleSheet s) {
        defaultStyles = s;
    }

    public StyleSheet getStyleSheet() {
        if (defaultStyles == null) {
            defaultStyles = new StyleSheet();
            defaultStyles.addRule(REGLAS_BASE);
        }
        return defaultStyles;
    }

    public Action[] getActions() {
        return javax.swing.text.TextAction.augmentList(super.getActions(), defaultActions);
    }

    protected void createInputAttributes(Element element, MutableAttributeSet set) {
        set.removeAttributes(set);
        set.addAttributes(element.getAttributes());
        set.removeAttribute(StyleConstants.ComposedTextAttribute);
    }

    public MutableAttributeSet getInputAttributes() {
        if (input == null) {
            input = new SimpleAttributeSet();
        }
        return input;
    }

    /** El cursor que se ve sobre el texto comun. */
    public void setDefaultCursor(Cursor cursor) {
        defaultCursor = cursor;
    }

    public Cursor getDefaultCursor() {
        return defaultCursor;
    }

    /** El cursor que se ve sobre un enlace. */
    public void setLinkCursor(Cursor cursor) {
        linkCursor = cursor;
    }

    public Cursor getLinkCursor() {
        return linkCursor;
    }

    /**
     * Si al enviar un formulario el juego mismo carga la respuesta.
     *
     * <p>Apagarlo hace que el envio llegue como un {@link FormSubmitEvent} y nada mas: quien
     * escucha decide que hacer. Es lo que hay que hacer si el documento puede venir de afuera,
     * porque si no cualquier pagina puede hacer que el programa pida una direccion.
     */
    public boolean isAutoFormSubmission() {
        return isAutoFormSubmission;
    }

    public void setAutoFormSubmission(boolean isAuto) {
        isAutoFormSubmission = isAuto;
    }

    public Object clone() {
        HTMLEditorKit o = (HTMLEditorKit) super.clone();
        if (o != null) {
            o.input = null;
            o.linkHandler = new LinkController();
        }
        return o;
    }

    /**
     * El analizador de HTML.
     *
     * <p>Se busca por reflexion; ver la nota de la clase. Si no esta, se devuelve nulo y quien lo
     * pidio va a fallar al leer, que es mejor que fallar al construir el juego de edicion.
     */
    protected Parser getParser() {
        if (defaultParser == null) {
            try {
                Class<?> c = Class.forName("javax.swing.text.html.parser.ParserDelegator");
                defaultParser = (Parser) c.getDeclaredConstructor().newInstance();
            } catch (Throwable e) {
                defaultParser = null;
            }
        }
        return defaultParser;
    }

    private static Parser defaultParser = null;

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /** Lo minimo de CSS que hace que un documento sin hoja se vea como HTML. */
    private static final String REGLAS_BASE =
            "body { margin-top: 8; margin-bottom: 8; margin-left: 8; margin-right: 8 }"
            + "p { margin-top: 5 }"
            + "h1 { font-size: 36pt; font-weight: bold; margin-top: 8; margin-bottom: 8 }"
            + "h2 { font-size: 24pt; font-weight: bold; margin-top: 8; margin-bottom: 8 }"
            + "h3 { font-size: 18pt; font-weight: bold; margin-top: 8; margin-bottom: 8 }"
            + "h4 { font-size: 14pt; font-weight: bold; margin-top: 8; margin-bottom: 8 }"
            + "h5 { font-size: 12pt; font-weight: bold; margin-top: 8; margin-bottom: 8 }"
            + "h6 { font-size: 10pt; font-weight: bold; margin-top: 8; margin-bottom: 8 }"
            + "b { font-weight: bold }"
            + "strong { font-weight: bold }"
            + "i { font-style: italic }"
            + "em { font-style: italic }"
            + "cite { font-style: italic }"
            + "u { text-decoration: underline }"
            + "tt { font-family: Monospaced }"
            + "code { font-family: Monospaced }"
            + "kbd { font-family: Monospaced }"
            + "samp { font-family: Monospaced }"
            + "pre { font-family: Monospaced; margin-top: 5 }"
            + "a { color: blue; text-decoration: underline }"
            + "ul { margin-left: 50; list-style-type: disc }"
            + "ol { margin-left: 50; list-style-type: decimal }"
            + "blockquote { margin-left: 40; margin-right: 40 }";

    private static final Action[] defaultActions = {
        new InsertHTMLTextAction("InsertTable", "<table border=1><tr><td></td></tr></table>",
                HTML.Tag.BODY, HTML.Tag.TABLE),
        new InsertHTMLTextAction("InsertTableRow", "<table border=1><tr><td></td></tr></table>",
                HTML.Tag.TABLE, HTML.Tag.TR, HTML.Tag.BODY, HTML.Tag.TABLE),
        new InsertHTMLTextAction("InsertTableDataCell",
                "<table border=1><tr><td></td></tr></table>",
                HTML.Tag.TR, HTML.Tag.TD, HTML.Tag.BODY, HTML.Tag.TABLE),
        new InsertHTMLTextAction("InsertUnorderedList", "<ul><li></li></ul>",
                HTML.Tag.BODY, HTML.Tag.UL),
        new InsertHTMLTextAction("InsertUnorderedListItem", "<ul><li></li></ul>",
                HTML.Tag.UL, HTML.Tag.LI, HTML.Tag.BODY, HTML.Tag.UL),
        new InsertHTMLTextAction("InsertOrderedList", "<ol><li></li></ol>",
                HTML.Tag.BODY, HTML.Tag.OL),
        new InsertHTMLTextAction("InsertOrderedListItem", "<ol><li></li></ol>",
                HTML.Tag.OL, HTML.Tag.LI, HTML.Tag.BODY, HTML.Tag.OL),
        new InsertHTMLTextAction("InsertPre", "<pre></pre>", HTML.Tag.BODY, HTML.Tag.PRE)
    };

    /**
     * Lo que un analizador de HTML tiene que saber hacer.
     *
     * <p>Es abstracta y de un solo metodo. Que sea una clase y no una interfaz es de 1998 y se
     * conserva porque {@link HTMLDocument#setParser} la recibe.
     */
    public abstract static class Parser {

        protected Parser() {
        }

        /**
         * Analiza el texto y le avisa a quien escucha.
         *
         * @param ignoreCharSet si hay que ignorar la codificacion que declare el documento.
         */
        public abstract void parse(Reader r, ParserCallback cb, boolean ignoreCharSet)
                throws IOException;
    }

    /**
     * Quien escucha lo que el analizador va encontrando.
     *
     * <p>Todos los metodos no hacen nada: se sobrescriben los que interesan. Que sea una clase con
     * cuerpos vacios y no una interfaz es lo que permite escuchar solo el texto sin escribir seis
     * metodos vacios.
     *
     * <p>El numero que llega en cada metodo es la posicion en el texto de entrada, no en el
     * documento. Sirve para senalar donde estaba un error.
     */
    public static class ParserCallback {

        /**
         * Marca los atributos de un elemento que el analizador invento.
         *
         * <p>Va como clave en el conjunto de atributos, con valor {@link Boolean#TRUE}. Quien
         * vuelva a escribir el documento la mira para no escribir etiquetas que el autor nunca
         * puso.
         */
        public static final Object IMPLIED = "_implied_";

        public ParserCallback() {
        }

        /** Se llama al terminar; es donde conviene volcar lo que se junto. */
        public void flush() throws BadLocationException {
        }

        public void handleText(char[] data, int pos) {
        }

        public void handleComment(char[] data, int pos) {
        }

        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        }

        public void handleEndTag(HTML.Tag t, int pos) {
        }

        /** Una etiqueta sin cierre, como {@code br} o {@code img}. */
        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        }

        public void handleError(String errorMsg, int pos) {
        }

        /** El fin de linea que usaba el documento, para poder escribirlo igual. */
        public void handleEndOfLineString(String eol) {
        }
    }

    /**
     * La fabrica de vistas: una vista por etiqueta.
     *
     * <p>El reparto no es arbitrario. Lo que arma bloque va a un {@link BlockView}, lo que va en la
     * linea a un {@link InlineView}, y las etiquetas que no tienen texto propio -- una imagen, un
     * campo de formulario -- a vistas que dibujan otra cosa.
     */
    public static class HTMLFactory implements ViewFactory {

        public HTMLFactory() {
        }

        public View create(Element elem) {
            AttributeSet attrs = elem.getAttributes();
            Object elementName = attrs.getAttribute(
                    javax.swing.text.AbstractDocument.ElementNameAttribute);
            Object o = (elementName != null) ? null
                    : attrs.getAttribute(StyleConstants.NameAttribute);
            if (o instanceof HTML.Tag) {
                HTML.Tag kind = (HTML.Tag) o;
                if (kind == HTML.Tag.CONTENT) {
                    return new InlineView(elem);
                } else if (kind == HTML.Tag.IMPLIED) {
                    return new ParagraphView(elem);
                } else if (kind == HTML.Tag.P || kind == HTML.Tag.H1 || kind == HTML.Tag.H2
                        || kind == HTML.Tag.H3 || kind == HTML.Tag.H4 || kind == HTML.Tag.H5
                        || kind == HTML.Tag.H6 || kind == HTML.Tag.DT) {
                    return new ParagraphView(elem);
                } else if (kind == HTML.Tag.MENU || kind == HTML.Tag.DIR || kind == HTML.Tag.UL
                        || kind == HTML.Tag.OL) {
                    return new ListView(elem);
                } else if (kind == HTML.Tag.BODY || kind == HTML.Tag.HTML
                        || kind == HTML.Tag.LI || kind == HTML.Tag.CENTER
                        || kind == HTML.Tag.DL || kind == HTML.Tag.DD || kind == HTML.Tag.DIV
                        || kind == HTML.Tag.BLOCKQUOTE || kind == HTML.Tag.PRE
                        || kind == HTML.Tag.FORM) {
                    return new BlockView(elem, View.Y_AXIS);
                } else if (kind == HTML.Tag.IMG) {
                    return new ImageView(elem);
                } else if (kind == HTML.Tag.INPUT || kind == HTML.Tag.SELECT
                        || kind == HTML.Tag.TEXTAREA) {
                    return new FormView(elem);
                } else if (kind == HTML.Tag.OBJECT) {
                    return new ObjectView(elem);
                } else if (kind == HTML.Tag.COMMENT || kind == HTML.Tag.HEAD
                        || kind == HTML.Tag.TITLE || kind == HTML.Tag.META
                        || kind == HTML.Tag.LINK || kind == HTML.Tag.STYLE
                        || kind == HTML.Tag.SCRIPT || kind == HTML.Tag.AREA
                        || kind == HTML.Tag.MAP || kind == HTML.Tag.PARAM
                        || kind == HTML.Tag.APPLET) {
                    // Lo que no se ve igual ocupa un lugar en el arbol: una vista de cero tamano.
                    return new InvisibleView(elem);
                }
            }
            // Un elemento que no se conoce: si tiene hijos es un bloque, si no es texto.
            String nm = elem.getName();
            if (nm != null && nm.equals(javax.swing.text.AbstractDocument.ContentElementName)) {
                return new javax.swing.text.LabelView(elem);
            }
            if (elem.isLeaf()) {
                return new javax.swing.text.LabelView(elem);
            }
            return new javax.swing.text.BoxView(elem, View.Y_AXIS);
        }
    }

    /** Una vista que ocupa cero: la de las etiquetas que no se muestran. */
    static class InvisibleView extends View {

        InvisibleView(Element elem) {
            super(elem);
        }

        public float getPreferredSpan(int axis) {
            return 0;
        }

        public void paint(java.awt.Graphics g, java.awt.Shape allocation) {
        }

        public java.awt.Shape modelToView(int pos, java.awt.Shape a,
                javax.swing.text.Position.Bias b) throws BadLocationException {
            return a;
        }

        public int viewToModel(float x, float y, java.awt.Shape a,
                javax.swing.text.Position.Bias[] bias) {
            bias[0] = javax.swing.text.Position.Bias.Forward;
            return getStartOffset();
        }
    }

    /**
     * Atiende los clics sobre enlaces.
     *
     * <p>Cambia el cursor al pasar por encima y dispara el evento al hacer clic. Que sea una clase
     * aparte y no codigo del juego de edicion permite que un panel tenga otro comportamiento sin
     * reemplazar todo el juego.
     */
    public static class LinkController extends MouseAdapter implements MouseMotionListener,
            Serializable {

        public LinkController() {
        }

        public void mouseClicked(MouseEvent e) {
            JEditorPane editor = (JEditorPane) e.getSource();
            if (!editor.isEditable() && javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                java.awt.Point pt = new java.awt.Point(e.getX(), e.getY());
                int pos = editor.viewToModel(pt);
                if (pos >= 0) {
                    activateLink(pos, editor);
                }
            }
        }

        public void mouseDragged(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
        }

        /** Dispara el evento del enlace que esta en esa posicion. */
        protected void activateLink(int pos, JEditorPane editor) {
        }
    }

    /**
     * La base de las acciones que editan HTML.
     *
     * <p>Trae lo que todas necesitan: llegar al documento y al juego, y buscar hacia arriba en el
     * arbol el elemento de una etiqueta dada. Eso ultimo es lo que permite escribir una accion como
     * "insertar una fila" sin saber donde esta la tabla.
     */
    public abstract static class HTMLTextAction extends StyledEditorKit.StyledTextAction {

        public HTMLTextAction(String name) {
            super(name);
        }

        protected HTMLDocument getHTMLDocument(JEditorPane e) {
            Document d = e.getDocument();
            if (d instanceof HTMLDocument) {
                return (HTMLDocument) d;
            }
            throw new IllegalArgumentException("document must be HTMLDocument");
        }

        protected HTMLEditorKit getHTMLEditorKit(JEditorPane e) {
            javax.swing.text.EditorKit k = e.getEditorKit();
            if (k instanceof HTMLEditorKit) {
                return (HTMLEditorKit) k;
            }
            throw new IllegalArgumentException("EditorKit must be HTMLEditorKit");
        }

        /** El camino de elementos desde la raiz hasta esa posicion. */
        protected Element[] getElementsAt(HTMLDocument doc, int offset) {
            return getElementsAt(doc.getDefaultRootElement(), offset, 0);
        }

        private Element[] getElementsAt(Element parent, int offset, int depth) {
            if (parent.isLeaf()) {
                Element[] retValue = new Element[depth + 1];
                retValue[depth] = parent;
                return retValue;
            }
            Element[] retValue = getElementsAt(parent.getElement(
                    parent.getElementIndex(offset)), offset, depth + 1);
            retValue[depth] = parent;
            return retValue;
        }

        /**
         * Cuantos elementos hay que cerrar para llegar a esa etiqueta.
         *
         * <p>Devuelve -1 si la etiqueta no esta en el camino. Es el numero que
         * {@link HTMLEditorKit#insertHTML} llama {@code popDepth}.
         */
        protected int elementCountToTag(HTMLDocument doc, int offset, HTML.Tag tag) {
            int depth = -1;
            Element e = doc.getCharacterElement(offset);
            while (e != null && e.getAttributes().getAttribute(
                    StyleConstants.NameAttribute) != tag) {
                e = e.getParentElement();
                depth++;
            }
            if (e == null) {
                return -1;
            }
            return depth;
        }

        /** El elemento mas cercano hacia arriba que tenga esa etiqueta. */
        protected Element findElementMatchingTag(HTMLDocument doc, int offset, HTML.Tag tag) {
            Element e = doc.getDefaultRootElement();
            Element lastMatch = null;
            while (e != null) {
                if (e.getAttributes().getAttribute(StyleConstants.NameAttribute) == tag) {
                    lastMatch = e;
                }
                e = e.getElement(e.getElementIndex(offset));
            }
            return lastMatch;
        }
    }

    /**
     * Inserta un fragmento de HTML en el lugar que corresponda.
     *
     * <p>Los dos pares de etiquetas son el motivo de que esta accion exista. El primero dice donde
     * se puede insertar y que se inserta; el segundo es el plan alternativo. Insertar un
     * <code>&lt;li&gt;</code> adentro de un <code>&lt;ul&gt;</code> agrega un renglon; si no hay
     * lista, el plan alternativo crea la lista entera.
     */
    public static class InsertHTMLTextAction extends HTMLTextAction {

        /** El HTML que se inserta. */
        protected String html;

        /** La etiqueta que tiene que contener a la insercion. */
        protected HTML.Tag parentTag;

        /** La etiqueta que se agrega. */
        protected HTML.Tag addTag;

        /** El contenedor del plan alternativo. */
        protected HTML.Tag alternateParentTag;

        /** Lo que se agrega en el plan alternativo. */
        protected HTML.Tag alternateAddTag;

        /** Una accion sin plan alternativo. */
        public InsertHTMLTextAction(String name, String html, HTML.Tag parentTag,
                HTML.Tag addTag) {
            this(name, html, parentTag, addTag, null, null);
        }

        /** Una accion con plan alternativo. */
        public InsertHTMLTextAction(String name, String html, HTML.Tag parentTag, HTML.Tag addTag,
                HTML.Tag alternateParentTag, HTML.Tag alternateAddTag) {
            super(name);
            this.html = html;
            this.parentTag = parentTag;
            this.addTag = addTag;
            this.alternateParentTag = alternateParentTag;
            this.alternateAddTag = alternateAddTag;
        }

        /** Hace la insercion; ver {@link HTMLEditorKit#insertHTML}. */
        protected void insertHTML(JEditorPane editor, HTMLDocument doc, int offset, String html,
                int popDepth, int pushDepth, HTML.Tag addTag) {
            try {
                getHTMLEditorKit(editor).insertHTML(doc, offset, html, popDepth, pushDepth,
                        addTag);
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                editor.getToolkit().beep();
            }
        }

        /**
         * Inserta cuando la posicion cae justo en el borde de un elemento.
         *
         * <p>Es el caso molesto: en el borde, la posicion pertenece a dos elementos y hay que
         * elegir. Se elige el de adentro, que es lo que espera quien esta escribiendo.
         */
        protected void insertAtBoundary(JEditorPane editor, HTMLDocument doc, int offset,
                Element insertElement, String html, HTML.Tag parentTag, HTML.Tag addTag) {
            insertAtBoundry(editor, doc, offset, insertElement, html, parentTag, addTag);
        }

        /**
         * El nombre viejo de {@link #insertAtBoundary}.
         *
         * @deprecated Usar {@link #insertAtBoundary}, que esta bien escrito.
         */
        @Deprecated
        protected void insertAtBoundry(JEditorPane editor, HTMLDocument doc, int offset,
                Element insertElement, String html, HTML.Tag parentTag, HTML.Tag addTag) {
            Element[] elements = getElementsAt(doc, offset);
            int depth = 0;
            for (int i = elements.length - 1; i >= 0; i--) {
                if (elements[i].getAttributes().getAttribute(
                        StyleConstants.NameAttribute) == parentTag) {
                    break;
                }
                depth++;
            }
            insertHTML(editor, doc, offset, html, depth, 0, addTag);
        }

        public void actionPerformed(ActionEvent ae) {
            JEditorPane editor = getEditor(ae);
            if (editor != null) {
                HTMLDocument doc = getHTMLDocument(editor);
                int offset = editor.getSelectionStart();
                int depth = elementCountToTag(doc, offset, parentTag);
                if (depth != -1) {
                    insertHTML(editor, doc, offset, html, depth, 0, addTag);
                } else if (alternateParentTag != null) {
                    depth = elementCountToTag(doc, offset, alternateParentTag);
                    if (depth != -1) {
                        insertHTML(editor, doc, offset, html, depth, 0, alternateAddTag);
                        return;
                    }
                    editor.getToolkit().beep();
                } else {
                    editor.getToolkit().beep();
                }
            }
        }
    }
}
