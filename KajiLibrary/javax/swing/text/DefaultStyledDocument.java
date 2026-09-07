package javax.swing.text;

import java.awt.Color;
import java.awt.Font;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$EventType;
import javax.swing.event.DocumentListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Un documento con estilos: seccion, parrafos y tramos de texto con atributos.
 *
 * <h2>Tres niveles</h2>
 *
 * <p>La raiz es una <em>seccion</em>; sus hijos son <em>parrafos</em>, uno por fin de linea; los
 * hijos de un parrafo son los tramos con los mismos atributos. Insertar texto con atributos
 * distintos a los de al lado parte un tramo en dos; escribir un fin de linea parte un parrafo.
 *
 * <h2>Como se aplica una edicion</h2>
 *
 * <p>{@link #insertUpdate} traduce la insercion a una lista de {@link ElementSpec} —"cerra este
 * parrafo", "abri otro", "meti este texto con estos atributos"— y {@link ElementBuffer} la aplica
 * sobre el arbol. Esa vuelta parece de mas y no lo es: la misma lista puede venir de otro lado
 * —de un lector de HTML, por ejemplo— y construir un arbol entero de una vez.
 *
 * <h2>Los limites de este ElementBuffer</h2>
 *
 * <p>El del JDK sabe <em>fracturar</em>: partir un parrafo en dos por el medio de una estructura
 * anidada, con las direcciones {@code JoinFractureDirection}. Ese caso lo produce el lector de
 * HTML, que no esta en esta biblioteca. Aca las direcciones de fractura se tratan como
 * {@code JoinNextDirection}, que es lo correcto para la estructura de tres niveles que este
 * documento arma, y esta dicho aca porque un dia puede no alcanzar.
 */
public class DefaultStyledDocument extends AbstractDocument implements StyledDocument {

    /** El tamano inicial del contenido de un documento con estilo. */
    public static final int BUFFER_SIZE_DEFAULT = 4096;

    /** Quien aplica las listas de {@link ElementSpec} sobre el arbol. */
    protected ElementBuffer buffer;

    private transient Vector<Style> listeningStyles = new Vector<Style>();
    private transient ChangeListener styleChangeListener;
    private transient ChangeListener styleContextChangeListener;
    private transient ChangeListener styleListener;

    /** Un documento sobre ese contenido y ese contexto de estilos. */
    public DefaultStyledDocument(Content c, StyleContext styles) {
        super(c, styles);
        listenerList = listenerList;
        buffer = new ElementBuffer(this, createDefaultRoot());
        Style defaultStyle = styles.getStyle(StyleContext.DEFAULT_STYLE);
        setLogicalStyle(0, defaultStyle);
    }

    public DefaultStyledDocument(StyleContext styles) {
        this(new GapContent(BUFFER_SIZE_DEFAULT), styles);
    }

    /** Un documento vacio con el contexto de estilos compartido. */
    public DefaultStyledDocument() {
        this(new GapContent(BUFFER_SIZE_DEFAULT), new StyleContext());
    }

    public Element getDefaultRootElement() {
        return buffer.getRootElement();
    }

    /**
     * Arma el documento entero desde una lista de especificaciones.
     *
     * <p>Borra lo que hubiera. Es como un lector construye un documento de una vez, sin pasar por
     * insercion tras insercion.
     */
    protected void create(ElementSpec[] data) {
        try {
            writeLock();

            // Sacar lo que hay.
            Element root = buffer.getRootElement();
            Element[] removed = new Element[root.getElementCount()];
            for (int i = 0; i < removed.length; i++) {
                removed[i] = root.getElement(i);
            }
            int len = getLength();
            if (len > 0) {
                getContent().remove(0, len);
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                ElementSpec es = data[i];
                if (es.getLength() > 0 && es.getType() == ElementSpec.ContentType) {
                    sb.append(es.getArray(), es.getOffset(), es.getLength());
                }
            }
            if (sb.length() > 0) {
                getContent().insertString(0, sb.toString());
            }

            DefaultDocumentEvent evnt = new DefaultDocumentEvent(this, 0, getLength(),
                    DocumentEvent$EventType.INSERT);
            buffer.create(getLength(), data, evnt);
            evnt.end();
            fireInsertUpdate(evnt);
            fireChangedUpdate(evnt);
        } catch (BadLocationException ble) {
            throw new StateInvariantError("problema creando el documento");
        } finally {
            writeUnlock();
        }
    }

    /** Inserta una lista de especificaciones en esa posicion. */
    protected void insert(int offset, ElementSpec[] data) throws BadLocationException {
        if (data == null || data.length == 0) {
            return;
        }
        try {
            writeLock();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                ElementSpec es = data[i];
                if (es.getLength() > 0 && es.getType() == ElementSpec.ContentType) {
                    sb.append(es.getArray(), es.getOffset(), es.getLength());
                }
            }
            int length = sb.length();
            if (length > 0) {
                getContent().insertString(offset, sb.toString());
            }
            DefaultDocumentEvent evnt = new DefaultDocumentEvent(this, offset, length,
                    DocumentEvent$EventType.INSERT);
            buffer.insert(offset, length, data, evnt);
            evnt.end();
            fireInsertUpdate(evnt);
        } finally {
            writeUnlock();
        }
    }

    /** Saca ese elemento del arbol, y con el su texto. */
    public void removeElement(Element elem) {
        try {
            removeElementImpl(elem);
        } catch (BadLocationException ble) {
            throw new IllegalArgumentException(ble.getMessage());
        }
    }

    private void removeElementImpl(Element elem) throws BadLocationException {
        if (elem.getDocument() != this) {
            throw new IllegalArgumentException("element doesn't belong to document");
        }
        BranchElement parent = (BranchElement) elem.getParentElement();
        if (parent == null) {
            throw new IllegalArgumentException("can't remove the root element");
        }
        int start = elem.getStartOffset();
        int end = Math.min(elem.getEndOffset(), getLength());
        remove(start, end - start);
    }

    public Style addStyle(String nm, Style parent) {
        StyleContext styles = (StyleContext) getAttributeContext();
        return styles.addStyle(nm, parent);
    }

    public void removeStyle(String nm) {
        StyleContext styles = (StyleContext) getAttributeContext();
        styles.removeStyle(nm);
    }

    public Style getStyle(String nm) {
        StyleContext styles = (StyleContext) getAttributeContext();
        return styles.getStyle(nm);
    }

    public Enumeration<?> getStyleNames() {
        return ((StyleContext) getAttributeContext()).getStyleNames();
    }

    /** Cuelga ese parrafo de ese estilo; ver {@link StyledDocument}. */
    public void setLogicalStyle(int pos, Style s) {
        Element paragraph = getParagraphElement(pos);
        if ((paragraph != null) && (paragraph instanceof AbstractElement)) {
            try {
                writeLock();
                StyleChangeUndoableEdit edit = new StyleChangeUndoableEdit(
                        (AbstractElement) paragraph, s);
                ((AbstractElement) paragraph).setResolveParent(s);
                int p0 = paragraph.getStartOffset();
                int p1 = paragraph.getEndOffset();
                DefaultDocumentEvent e = new DefaultDocumentEvent(this, p0, p1 - p0,
                        DocumentEvent$EventType.CHANGE);
                e.addEdit(edit);
                e.end();
                fireChangedUpdate(e);
                fireUndoableEditUpdate(new javax.swing.event.UndoableEditEvent(this, e));
            } finally {
                writeUnlock();
            }
        }
    }

    public Style getLogicalStyle(int p) {
        Style s = null;
        Element paragraph = getParagraphElement(p);
        if (paragraph != null) {
            AttributeSet a = paragraph.getAttributes();
            AttributeSet parent = a.getResolveParent();
            if (parent instanceof Style) {
                s = (Style) parent;
            }
        }
        return s;
    }

    /**
     * Aplica atributos de caracter a ese tramo.
     *
     * <p>Parte los tramos que el rango corta por el medio: despues de esto, cada hoja tiene
     * atributos uniformes, que es la invariante de este documento.
     */
    public void setCharacterAttributes(int offset, int length, AttributeSet s, boolean replace) {
        if (length == 0) {
            return;
        }
        try {
            writeLock();
            DefaultDocumentEvent changes = new DefaultDocumentEvent(this, offset, length,
                    DocumentEvent$EventType.CHANGE);

            buffer.change(offset, length, changes);

            AttributeSet sCopy = s.copyAttributes();
            int lastEnd;
            for (int pos = offset; pos < (offset + length); pos = lastEnd) {
                Element run = getCharacterElement(pos);
                lastEnd = run.getEndOffset();
                if (pos == lastEnd) {
                    break;
                }
                MutableAttributeSet attr = (MutableAttributeSet) run.getAttributes();
                changes.addEdit(new AttributeUndoableEdit(run, sCopy, replace));
                if (replace) {
                    attr.removeAttributes(attr);
                }
                attr.addAttributes(s);
            }
            changes.end();
            fireChangedUpdate(changes);
            fireUndoableEditUpdate(new javax.swing.event.UndoableEditEvent(this, changes));
        } finally {
            writeUnlock();
        }
    }

    /** Aplica atributos a los parrafos que toca ese tramo, enteros. */
    public void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace) {
        try {
            writeLock();
            DefaultDocumentEvent changes = new DefaultDocumentEvent(this, offset, length,
                    DocumentEvent$EventType.CHANGE);

            AttributeSet sCopy = s.copyAttributes();
            Element section = getDefaultRootElement();
            int index0 = section.getElementIndex(offset);
            int index1 = section.getElementIndex(offset + ((length > 0) ? length - 1 : 0));
            for (int i = index0; i <= index1; i++) {
                Element paragraph = section.getElement(i);
                MutableAttributeSet attr = (MutableAttributeSet) paragraph.getAttributes();
                changes.addEdit(new AttributeUndoableEdit(paragraph, sCopy, replace));
                if (replace) {
                    attr.removeAttributes(attr);
                }
                attr.addAttributes(s);
            }
            changes.end();
            fireChangedUpdate(changes);
            fireUndoableEditUpdate(new javax.swing.event.UndoableEditEvent(this, changes));
        } finally {
            writeUnlock();
        }
    }

    public Element getParagraphElement(int pos) {
        Element e;
        for (e = getDefaultRootElement(); !e.isLeaf();) {
            int index = e.getElementIndex(pos);
            e = e.getElement(index);
        }
        if (e != null) {
            return e.getParentElement();
        }
        return e;
    }

    public Element getCharacterElement(int pos) {
        Element e;
        for (e = getDefaultRootElement(); !e.isLeaf();) {
            int index = e.getElementIndex(pos);
            e = e.getElement(index);
        }
        return e;
    }

    /**
     * Acomoda el arbol despues de una insercion.
     *
     * <p>Arma la lista de especificaciones: el texto va como contenido, y cada fin de linea se
     * traduce en cerrar el parrafo y abrir otro.
     */
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
        int offset = chng.getOffset();
        int length = chng.getLength();
        if (attr == null) {
            attr = SimpleAttributeSet.EMPTY;
        }

        Element paragraph = getParagraphElement(offset);
        AttributeSet pattr = paragraph.getAttributes();

        Segment s = new Segment();
        try {
            getText(offset, length, s);
        } catch (BadLocationException e) {
            throw new StateInvariantError("problema leyendo lo insertado");
        }

        Vector<ElementSpec> parseBuffer = new Vector<ElementSpec>();
        int lastOffset = 0;
        for (int i = 0; i < length; i++) {
            char c = s.array[s.offset + i];
            if (c == '\n') {
                int tramo = i - lastOffset + 1;
                parseBuffer.addElement(new ElementSpec(attr, ElementSpec.ContentType,
                        s.array, s.offset + lastOffset, tramo));
                parseBuffer.addElement(new ElementSpec(null, ElementSpec.EndTagType));
                parseBuffer.addElement(new ElementSpec(pattr, ElementSpec.StartTagType));
                lastOffset = i + 1;
            }
        }
        if (lastOffset < length) {
            parseBuffer.addElement(new ElementSpec(attr, ElementSpec.ContentType,
                    s.array, s.offset + lastOffset, length - lastOffset));
        }

        ElementSpec[] spec = new ElementSpec[parseBuffer.size()];
        parseBuffer.copyInto(spec);
        if (spec.length > 0) {
            spec[0].setDirection(ElementSpec.JoinPreviousDirection);
            if (spec.length > 1 && spec[spec.length - 1].getType() == ElementSpec.ContentType) {
                spec[spec.length - 1].setDirection(ElementSpec.JoinNextDirection);
            }
        }
        buffer.insert(offset, length, spec, chng);
        super.insertUpdate(chng, attr);
    }

    /**
     * Arma las especificaciones de una insercion despues de un fin de linea.
     *
     * <p>Devuelve la direccion que le corresponde al primer trozo. Es lo que decide si el texto
     * nuevo se pega al parrafo de arriba o abre uno propio.
     */
    short createSpecsForInsertAfterNewline(Element paragraph, Element pParagraph,
            AttributeSet pattr, Vector<ElementSpec> parseBuffer, int offset, int endOffset) {
        if (paragraph.getStartOffset() == offset) {
            return ElementSpec.JoinNextDirection;
        }
        return ElementSpec.OriginateDirection;
    }

    /**
     * Junta los parrafos que el borrado dejo a medias.
     *
     * <p>Corre <em>antes</em> de que el texto se vaya, que es cuando todavia se puede saber
     * cuantos parrafos tocaba el tramo: una vez borrado, los dos extremos caen en el mismo lugar.
     */
    protected void removeUpdate(DefaultDocumentEvent chng) {
        super.removeUpdate(chng);
        buffer.remove(chng.getOffset(), chng.getLength(), chng);
    }

    /**
     * Saca las hojas que el borrado dejo vacias.
     *
     * <p>Corre <em>despues</em>, que es cuando las hojas de lo borrado ya colapsaron a largo
     * cero. Antes no habria nada que sacar.
     */
    protected void postRemoveUpdate(DefaultDocumentEvent chng) {
        super.postRemoveUpdate(chng);
        buffer.limpiar(chng.getOffset(), chng);
    }

    /** La seccion raiz, con un parrafo con un tramo vacio adentro. */
    protected AbstractElement createDefaultRoot() {
        writeLock();
        BranchElement section = new SectionElement(this);
        BranchElement paragraph = new BranchElement(this, section, null);

        LeafElement brk = new LeafElement(this, paragraph, null, 0, 1);
        Element[] buff = new Element[1];
        buff[0] = brk;
        paragraph.replace(0, 0, buff);

        buff[0] = paragraph;
        section.replace(0, 0, buff);
        writeUnlock();
        return section;
    }

    public Color getForeground(AttributeSet attr) {
        StyleContext styles = (StyleContext) getAttributeContext();
        return styles.getForeground(attr);
    }

    public Color getBackground(AttributeSet attr) {
        StyleContext styles = (StyleContext) getAttributeContext();
        return styles.getBackground(attr);
    }

    public Font getFont(AttributeSet attr) {
        StyleContext styles = (StyleContext) getAttributeContext();
        return styles.getFont(attr);
    }

    /**
     * Avisa que un estilo cambio: todo lo que cuelga de el se tiene que repintar.
     *
     * <p>El evento cubre el documento entero porque el estilo puede estar usado en cualquier
     * lado. Es caro y es lo que hace el JDK: buscar exactamente donde se usa costaria mas que
     * repintar.
     */
    protected void styleChanged(Style style) {
        DefaultDocumentEvent dde = new DefaultDocumentEvent(this, 0, getLength(),
                DocumentEvent$EventType.CHANGE);
        dde.end();
        fireChangedUpdate(dde);
    }

    /** Al primer escucha, este documento empieza a escuchar a los estilos. */
    public void addDocumentListener(DocumentListener listener) {
        synchronized (listeningStyles) {
            int oldDLCount = listenerList.getListenerCount(DocumentListener.class);
            super.addDocumentListener(listener);
            if (oldDLCount == 0) {
                if (styleContextChangeListener == null) {
                    styleContextChangeListener = createStyleContextChangeListener();
                }
                if (styleContextChangeListener != null) {
                    StyleContext styles = (StyleContext) getAttributeContext();
                    styles.addChangeListener(styleContextChangeListener);
                }
                updateStylesListeningTo();
            }
        }
    }

    /** Sin escuchas, deja de escuchar a los estilos: nadie se enteraria. */
    public void removeDocumentListener(DocumentListener listener) {
        synchronized (listeningStyles) {
            super.removeDocumentListener(listener);
            if (listenerList.getListenerCount(DocumentListener.class) == 0) {
                for (int counter = listeningStyles.size() - 1; counter >= 0; counter--) {
                    listeningStyles.elementAt(counter).removeChangeListener(styleChangeListener);
                }
                listeningStyles.removeAllElements();
                if (styleContextChangeListener != null) {
                    StyleContext styles = (StyleContext) getAttributeContext();
                    styles.removeChangeListener(styleContextChangeListener);
                }
            }
        }
    }

    ChangeListener createStyleChangeListener() {
        return new StyleChangeHandler(this);
    }

    ChangeListener createStyleContextChangeListener() {
        return new StyleContextChangeHandler(this);
    }

    /** Vuelve a mirar de que estilos cuelgan los parrafos, y escucha a esos. */
    void updateStylesListeningTo() {
        synchronized (listeningStyles) {
            StyleContext styles = (StyleContext) getAttributeContext();
            if (styleChangeListener == null) {
                styleChangeListener = createStyleChangeListener();
            }
            if (styleChangeListener != null && styles != null) {
                Element root = getDefaultRootElement();
                Vector<Style> vistos = new Vector<Style>();
                for (int i = 0; i < root.getElementCount(); i++) {
                    Element p = root.getElement(i);
                    AttributeSet parent = p.getAttributes().getResolveParent();
                    if (parent instanceof Style) {
                        Style s = (Style) parent;
                        if (!vistos.contains(s)) {
                            vistos.addElement(s);
                        }
                    }
                }
                for (int i = listeningStyles.size() - 1; i >= 0; i--) {
                    Style s = listeningStyles.elementAt(i);
                    if (!vistos.contains(s)) {
                        s.removeChangeListener(styleChangeListener);
                        listeningStyles.removeElementAt(i);
                    }
                }
                for (int i = 0; i < vistos.size(); i++) {
                    Style s = vistos.elementAt(i);
                    if (!listeningStyles.contains(s)) {
                        s.addChangeListener(styleChangeListener);
                        listeningStyles.addElement(s);
                    }
                }
            }
        }
    }

    /**
     * Una instruccion de estructura: abrir, cerrar o poner contenido.
     *
     * <p>Es el idioma en el que se le habla al {@link ElementBuffer}. Un documento se puede armar
     * entero con una lista de estas, y por eso un lector de un formato ajeno no necesita saber
     * nada del arbol: solo emitir esta secuencia.
     */
    public static class ElementSpec {

        /** Abre un elemento. */
        public static final short StartTagType = 1;

        /** Cierra el elemento abierto. */
        public static final short EndTagType = 2;

        /** Pone texto. */
        public static final short ContentType = 3;

        /** Se pega a lo que habia antes. */
        public static final short JoinPreviousDirection = 4;

        /** Se pega a lo que viene despues. */
        public static final short JoinNextDirection = 5;

        /** Empieza algo propio. */
        public static final short OriginateDirection = 6;

        /** Se pega a lo que quedo de una fractura; ver la nota de {@link DefaultStyledDocument}. */
        public static final short JoinFractureDirection = 7;

        private AttributeSet attr;
        private int len;
        private short type;
        private short direction;
        private int offs;
        private char[] data;

        /** Una instruccion sin contenido: abrir o cerrar. */
        public ElementSpec(AttributeSet a, short type) {
            this(a, type, 0);
        }

        /** Una instruccion de contenido de ese largo, sin texto asociado todavia. */
        public ElementSpec(AttributeSet a, short type, int len) {
            attr = a;
            this.type = type;
            this.len = len;
            this.direction = OriginateDirection;
        }

        /** Una instruccion de contenido con su texto. */
        public ElementSpec(AttributeSet a, short type, char[] txt, int offs, int len) {
            attr = a;
            this.type = type;
            this.data = txt;
            this.offs = offs;
            this.len = len;
            this.direction = OriginateDirection;
        }

        public void setType(short type) {
            this.type = type;
        }

        public short getType() {
            return type;
        }

        public void setDirection(short direction) {
            this.direction = direction;
        }

        public short getDirection() {
            return direction;
        }

        public AttributeSet getAttributes() {
            return attr;
        }

        public char[] getArray() {
            return data;
        }

        public int getOffset() {
            return offs;
        }

        public int getLength() {
            return len;
        }

        public String toString() {
            String tipo = "??";
            if (type == StartTagType) {
                tipo = "StartTag";
            } else if (type == ContentType) {
                tipo = "Content";
            } else if (type == EndTagType) {
                tipo = "EndTag";
            }
            String dir = "??";
            if (direction == JoinPreviousDirection) {
                dir = "JoinPrevious";
            } else if (direction == JoinNextDirection) {
                dir = "JoinNext";
            } else if (direction == OriginateDirection) {
                dir = "Originate";
            } else if (direction == JoinFractureDirection) {
                dir = "Fracture";
            }
            return tipo + ":" + dir + ":" + getLength();
        }
    }

    /**
     * Aplica listas de {@link ElementSpec} sobre el arbol.
     *
     * <p>Lleva la cuenta de que elementos se fueron y cuales llegaron, y los anota en el evento:
     * de ahi salen los {@code ElementChange} que un editor usa para repintar solo lo que cambio.
     *
     * <p>Ver la nota de {@link DefaultStyledDocument} sobre lo que este buffer no sabe hacer.
     */
    public static class ElementBuffer implements Serializable {

        /** La raiz sobre la que trabaja. */
        Element root;

        private final DefaultStyledDocument documento;

        transient int pos;
        transient int offset;
        transient int length;
        transient int endOffset;
        transient boolean insertOp;

        /** Un buffer sobre esa raiz. */
        public ElementBuffer(DefaultStyledDocument documento, Element root) {
            this.documento = documento;
            this.root = root;
        }

        public Element getRootElement() {
            return root;
        }

        /** Aplica una insercion; ver {@link #insertUpdate}. */
        public void insert(int offset, int length, ElementSpec[] data,
                DefaultDocumentEvent de) {
            if (length == 0) {
                return;
            }
            this.offset = offset;
            this.pos = offset;
            this.endOffset = offset + length;
            this.length = length;
            this.evento = de;
            insertOp = true;
            insertUpdate(data);
            insertOp = false;
            this.evento = null;
        }

        /** Arma el arbol entero desde la lista. */
        public void create(int length, ElementSpec[] data, DefaultDocumentEvent de) {
            this.offset = 0;
            this.pos = 0;
            this.length = length;
            this.endOffset = length;
            this.evento = de;

            BranchElement seccion = (BranchElement) root;
            Element[] viejos = new Element[seccion.getElementCount()];
            for (int i = 0; i < viejos.length; i++) {
                viejos[i] = seccion.getElement(i);
            }

            Vector<Element> parrafos = new Vector<Element>();
            BranchElement actual = null;
            int p = 0;
            for (int i = 0; i < data.length; i++) {
                ElementSpec spec = data[i];
                if (spec.getType() == ElementSpec.StartTagType) {
                    actual = new BranchElement(documento, seccion, spec.getAttributes());
                    parrafos.addElement(actual);
                } else if (spec.getType() == ElementSpec.ContentType) {
                    if (actual == null) {
                        actual = new BranchElement(documento, seccion, null);
                        parrafos.addElement(actual);
                    }
                    Element hoja = new LeafElement(documento, actual, spec.getAttributes(), p,
                            p + spec.getLength());
                    agregar(actual, hoja);
                    p = p + spec.getLength();
                }
            }
            if (parrafos.size() == 0) {
                actual = new BranchElement(documento, seccion, null);
                agregar(actual, new LeafElement(documento, actual, null, 0, length));
                parrafos.addElement(actual);
            }
            Element[] nuevos = new Element[parrafos.size()];
            parrafos.copyInto(nuevos);
            seccion.replace(0, viejos.length, nuevos);
            de.addEdit(new ElementEdit(seccion, 0, viejos, nuevos));
            this.evento = null;
        }

        /** Aplica un borrado; ver {@link #removeUpdate}. */
        public void remove(int offset, int length, DefaultDocumentEvent de) {
            this.offset = offset;
            this.length = length;
            this.endOffset = offset + length;
            this.evento = de;
            insertOp = false;
            removeUpdate();
            this.evento = null;
        }

        /** Saca del parrafo de esa posicion las hojas que quedaron vacias. */
        void limpiar(int offset, DefaultDocumentEvent de) {
            this.evento = de;
            BranchElement seccion = (BranchElement) root;
            int i = seccion.getElementIndex(offset);
            Element p = seccion.getElement(i);
            if (p instanceof BranchElement) {
                limpiarVacias((BranchElement) p);
            }
            this.evento = null;
        }

        /** Marca un tramo como cambiado, partiendo las hojas que el rango corta. */
        public void change(int offset, int length, DefaultDocumentEvent de) {
            this.offset = offset;
            this.length = length;
            this.endOffset = offset + length;
            this.evento = de;
            changeUpdate();
            this.evento = null;
        }

        private transient DefaultDocumentEvent evento;

        /**
         * Aplica la lista sobre el arbol, en tres pasos.
         *
         * <p>Primero le pone a cada tramo insertado sus atributos, partiendo las hojas que
         * queden a medias; despues corta el parrafo en cada fin de linea.
         *
         * <p>Dos hojas pegadas con los mismos atributos <strong>no</strong> se juntan, y es a
         * proposito: el JDK tampoco las junta, y de ahi que un documento recien escrito tenga un
         * tramo aparte para el fin de linea del final. Juntarlas cambiaria la cuenta de hijos que
         * ve cualquiera que recorra el arbol.
         */
        protected void insertUpdate(ElementSpec[] data) {
            BranchElement seccion = (BranchElement) root;
            int indiceParrafo = seccion.getElementIndex(offset);
            BranchElement parrafo = (BranchElement) seccion.getElement(indiceParrafo);

            int p = offset;
            Vector<Integer> cortes = new Vector<Integer>();
            for (int i = 0; i < data.length; i++) {
                ElementSpec spec = data[i];
                short tipo = spec.getType();
                if (tipo == ElementSpec.ContentType) {
                    aplicarAtributos(parrafo, p, p + spec.getLength(), spec.getAttributes());
                    p = p + spec.getLength();
                } else if (tipo == ElementSpec.EndTagType) {
                    cortes.addElement(Integer.valueOf(p));
                }
            }

            BranchElement actual = parrafo;
            int indiceActual = indiceParrafo;
            for (int i = 0; i < cortes.size(); i++) {
                int corte = cortes.elementAt(i).intValue();
                actual = cortarParrafo(seccion, indiceActual, actual, corte);
                indiceActual = indiceActual + 1;
            }
        }

        /** Le pone esos atributos al tramo, partiendo las hojas que queden a medias. */
        private void aplicarAtributos(BranchElement parrafo, int desde, int hasta,
                AttributeSet attr) {
            if (hasta <= desde) {
                return;
            }
            AttributeSet nuevos = (attr == null) ? SimpleAttributeSet.EMPTY : attr;
            int p = desde;
            while (p < hasta) {
                int indice = parrafo.getElementIndex(p);
                Element hoja = parrafo.getElement(indice);
                if (hoja == null) {
                    return;
                }
                int hd = hoja.getStartOffset();
                int hh = hoja.getEndOffset();
                if (hh <= p) {
                    return;
                }
                int corte = Math.min(hh, hasta);
                // Ya tiene esos atributos: no hay nada que cambiar y no se parte. Es lo que hace
                // que insertar texto sin estilo dentro de un tramo sin estilo no deje un corte.
                if (hoja.getAttributes().isEqual(nuevos)) {
                    p = corte;
                    continue;
                }
                Vector<Element> nuevas = new Vector<Element>();
                if (hd < p) {
                    nuevas.addElement(new LeafElement(documento, parrafo, hoja.getAttributes(),
                            hd, p));
                }
                nuevas.addElement(new LeafElement(documento, parrafo, nuevos, p, corte));
                if (hh > corte) {
                    nuevas.addElement(new LeafElement(documento, parrafo, hoja.getAttributes(),
                            corte, hh));
                }
                Element[] agregadas = new Element[nuevas.size()];
                nuevas.copyInto(agregadas);
                Element[] viejas = new Element[] {hoja};
                parrafo.replace(indice, 1, agregadas);
                anotar(parrafo, indice, viejas, agregadas);
                p = corte;
            }
        }

        /**
         * Corta el parrafo en esa posicion y devuelve el de la derecha.
         *
         * <p>El de la izquierda se queda con el mismo objeto —asi las vistas que lo tenian siguen
         * valiendo— y el de la derecha es nuevo, con los mismos atributos.
         */
        private BranchElement cortarParrafo(BranchElement seccion, int indiceParrafo,
                BranchElement parrafo, int corte) {
            int fin = parrafo.getEndOffset();
            if (corte >= fin) {
                return parrafo;
            }
            Vector<Element> izquierda = new Vector<Element>();
            Vector<Element> derecha = new Vector<Element>();
            int n = parrafo.getElementCount();
            Element[] viejas = new Element[n];
            for (int i = 0; i < n; i++) {
                Element h = parrafo.getElement(i);
                viejas[i] = h;
                int hd = h.getStartOffset();
                int hh = h.getEndOffset();
                if (hh <= corte) {
                    izquierda.addElement(h);
                } else if (hd >= corte) {
                    derecha.addElement(h);
                } else {
                    izquierda.addElement(new LeafElement(documento, parrafo, h.getAttributes(),
                            hd, corte));
                    derecha.addElement(new LeafElement(documento, parrafo, h.getAttributes(),
                            corte, hh));
                }
            }

            BranchElement resto = new BranchElement(documento, seccion, parrafo.getAttributes());
            Element[] izq = new Element[izquierda.size()];
            izquierda.copyInto(izq);
            if (izq.length == 0) {
                izq = new Element[] {new LeafElement(documento, parrafo, null,
                        parrafo.getStartOffset(), corte)};
            }
            parrafo.replace(0, n, izq);
            anotar(parrafo, 0, viejas, izq);

            Element[] der = new Element[derecha.size()];
            derecha.copyInto(der);
            if (der.length == 0) {
                der = new Element[] {new LeafElement(documento, resto, null, corte, fin)};
            }
            resto.replace(0, 0, der);

            Element[] agregados = new Element[] {resto};
            seccion.replace(indiceParrafo + 1, 0, agregados);
            anotar(seccion, indiceParrafo + 1, new Element[0], agregados);
            return resto;
        }

        /** Anota un cambio de hijos en el evento en curso, si hay. */
        private void anotar(Element padre, int indice, Element[] viejos, Element[] nuevos) {
            if (evento != null) {
                evento.addEdit(new ElementEdit(padre, indice, viejos, nuevos));
            }
        }

        /** Junta los parrafos que el borrado dejo a medias. */
        protected void removeUpdate() {
            BranchElement seccion = (BranchElement) root;
            int i0 = seccion.getElementIndex(offset);
            int i1 = seccion.getElementIndex(endOffset);
            if (i0 == i1) {
                limpiarVacias((BranchElement) seccion.getElement(i0));
                anotarCambio(seccion.getElement(i0));
                return;
            }

            Element[] viejos = new Element[i1 - i0 + 1];
            for (int i = i0; i <= i1; i++) {
                viejos[i - i0] = seccion.getElement(i);
            }

            BranchElement primero = (BranchElement) seccion.getElement(i0);
            Vector<Element> hojas = new Vector<Element>();
            for (int i = i0; i <= i1; i++) {
                Element p = seccion.getElement(i);
                for (int j = 0; j < p.getElementCount(); j++) {
                    Element h = p.getElement(j);
                    if (h.getEndOffset() > h.getStartOffset()) {
                        hojas.addElement(new LeafElement(documento, primero, h.getAttributes(),
                                h.getStartOffset(), h.getEndOffset()));
                    }
                }
            }
            Element[] nuevasHojas = new Element[hojas.size()];
            hojas.copyInto(nuevasHojas);
            if (nuevasHojas.length == 0) {
                nuevasHojas = new Element[] {new LeafElement(documento, primero, null,
                        primero.getStartOffset(), primero.getStartOffset() + 1)};
            }
            primero.replace(0, primero.getElementCount(), nuevasHojas);

            Element[] nuevos = new Element[] {primero};
            seccion.replace(i0, viejos.length, nuevos);
            anotar(seccion, i0, viejos, nuevos);
            limpiarVacias(primero);
        }

        /** Parte las hojas que el rango corta, para que los atributos queden uniformes. */
        protected void changeUpdate() {
            BranchElement seccion = (BranchElement) root;
            int p = offset;
            while (p < endOffset) {
                Element parrafo = documento.getParagraphElement(p);
                BranchElement br = (BranchElement) parrafo;
                int indice = br.getElementIndex(p);
                Element hoja = br.getElement(indice);
                int hd = hoja.getStartOffset();
                int hh = hoja.getEndOffset();
                int corte = Math.min(hh, endOffset);
                if (hd < p || hh > corte) {
                    Vector<Element> nuevas = new Vector<Element>();
                    if (hd < p) {
                        nuevas.addElement(new LeafElement(documento, br, hoja.getAttributes(),
                                hd, p));
                    }
                    nuevas.addElement(new LeafElement(documento, br, hoja.getAttributes(), p,
                            corte));
                    if (hh > corte) {
                        nuevas.addElement(new LeafElement(documento, br, hoja.getAttributes(),
                                corte, hh));
                    }
                    Element[] agregadas = new Element[nuevas.size()];
                    nuevas.copyInto(agregadas);
                    Element[] viejas = new Element[] {hoja};
                    br.replace(indice, 1, agregadas);
                    anotar(br, indice, viejas, agregadas);
                }
                p = corte;
                if (corte <= hd) {
                    break;
                }
            }
        }

        /**
         * Saca las hojas que quedaron de largo cero.
         *
         * <p>Un borrado que se come un tramo entero deja su hoja con principio y fin en el mismo
         * lugar. No molesta al texto, pero si a todo lo que recorre el arbol: una hoja vacia no
         * representa nada. Si se van todas, queda una que cubre el parrafo.
         */
        private void limpiarVacias(BranchElement parrafo) {
            int n = parrafo.getElementCount();
            Vector<Element> vivas = new Vector<Element>();
            for (int i = 0; i < n; i++) {
                Element h = parrafo.getElement(i);
                if (h.getEndOffset() > h.getStartOffset()) {
                    vivas.addElement(h);
                }
            }
            if (vivas.size() == n) {
                return;
            }
            Element[] viejas = new Element[n];
            for (int i = 0; i < n; i++) {
                viejas[i] = parrafo.getElement(i);
            }
            Element[] nuevas;
            if (vivas.size() == 0) {
                nuevas = new Element[] {new LeafElement(documento, parrafo, null,
                        parrafo.getStartOffset(), parrafo.getEndOffset())};
            } else {
                nuevas = new Element[vivas.size()];
                vivas.copyInto(nuevas);
            }
            parrafo.replace(0, n, nuevas);
            anotar(parrafo, 0, viejas, nuevas);
        }

        /** Anota que ese elemento cambio sin cambiar de hijos. */
        private void anotarCambio(Element e) {
            if (e != null) {
                anotar(e, 0, new Element[0], new Element[0]);
            }
        }

        /** Agrega una hoja al final de un parrafo. */
        private void agregar(BranchElement parrafo, Element hoja) {
            Element[] uno = new Element[] {hoja};
            parrafo.replace(parrafo.getElementCount(), 0, uno);
        }
    }

    /** La raiz de un documento con estilo; su nombre la distingue de un parrafo. */
    protected class SectionElement extends BranchElement {

        public SectionElement(DefaultStyledDocument documento) {
            super(documento, null, null);
        }

        public String getName() {
            return SectionElementName;
        }
    }

    /** Deshacer un cambio de atributos: se guarda la copia de como estaban. */
    public static class AttributeUndoableEdit extends AbstractUndoableEdit {

        protected AttributeSet newAttributes;
        protected AttributeSet copy;
        protected boolean isReplacing;
        protected Element element;

        public AttributeUndoableEdit(Element element, AttributeSet newAttributes,
                boolean isReplacing) {
            super();
            this.element = element;
            this.newAttributes = newAttributes;
            this.isReplacing = isReplacing;
            copy = element.getAttributes().copyAttributes();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            MutableAttributeSet as = (MutableAttributeSet) element.getAttributes();
            if (isReplacing) {
                as.removeAttributes(as);
            }
            as.addAttributes(newAttributes);
        }

        public void undo() throws CannotUndoException {
            super.undo();
            MutableAttributeSet as = (MutableAttributeSet) element.getAttributes();
            as.removeAttributes(as);
            as.addAttributes(copy);
        }
    }

    /** Deshacer un cambio de estilo logico. */
    static class StyleChangeUndoableEdit extends AbstractUndoableEdit {

        StyleChangeUndoableEdit(AbstractElement element, Style newStyle) {
            super();
            this.element = element;
            this.newStyle = newStyle;
            oldStyle = element.getResolveParent();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            element.setResolveParent(newStyle);
        }

        public void undo() throws CannotUndoException {
            super.undo();
            element.setResolveParent(oldStyle);
        }

        protected AbstractElement element;
        protected Style newStyle;
        protected AttributeSet oldStyle;
    }

    /** Un estilo cambio: el documento avisa que hay que repintar. */
    static class StyleChangeHandler implements ChangeListener, Serializable {

        private final DefaultStyledDocument documento;

        StyleChangeHandler(DefaultStyledDocument documento) {
            this.documento = documento;
        }

        public void stateChanged(ChangeEvent e) {
            Object source = e.getSource();
            if (source instanceof Style) {
                documento.styleChanged((Style) source);
            } else {
                documento.styleChanged(null);
            }
        }
    }

    /** Cambio el juego de estilos: hay que volver a mirar de cuales cuelgan los parrafos. */
    static class StyleContextChangeHandler implements ChangeListener, Serializable {

        private final DefaultStyledDocument documento;

        StyleContextChangeHandler(DefaultStyledDocument documento) {
            this.documento = documento;
        }

        public void stateChanged(ChangeEvent e) {
            documento.updateStylesListeningTo();
        }
    }
}
