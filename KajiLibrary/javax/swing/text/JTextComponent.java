package javax.swing.text;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.im.InputMethodRequests;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Vector;

import javax.accessibility.AccessibleContext;
import javax.print.PrintService;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler$DropLocation;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.plaf.TextUI;

/**
 * La base de todo componente que muestra o edita texto.
 *
 * <h2>Cuatro piezas y ninguna es esta clase</h2>
 *
 * <p>El componente no guarda el texto, ni sabe dibujarlo, ni sabe donde esta el cursor. Guarda
 * referencias a los cuatro que si saben:
 *
 * <ul>
 * <li>El {@link Document}, que tiene el texto y la estructura.
 * <li>El {@link Caret}, que sabe donde se escribe y que hay seleccionado.
 * <li>El {@link Highlighter}, que pinta los fondos.
 * <li>El {@link TextUI}, que tiene el arbol de vistas y traduce entre texto y pantalla.
 * </ul>
 *
 * <p>Casi todos los metodos de aca son eso: preguntarle a alguno de los cuatro. {@code setText} es
 * un {@code remove} mas un {@code insertString} sobre el documento; {@code modelToView} es una
 * llamada al UI; {@code getSelectionStart} es el menor entre el punto y la marca del cursor. Que
 * la clase sea tan grande y tan delgada a la vez es intencional: es la fachada.
 *
 * <h2>Los mapas de teclas</h2>
 *
 * <p>Los {@link Keymap} se guardan en una tabla estatica compartida por nombre. Es de las pocas
 * cosas globales que quedan en Swing, y viene de antes de {@code InputMap}: un editor podia
 * nombrar su mapa y otro pedirlo por ese nombre.
 *
 * <h2>Lo que no esta</h2>
 *
 * <p>{@link #cut}, {@link #copy} y {@link #paste} necesitan un portapapeles del sistema, e
 * {@link #print} una impresora: esta VM no tiene ninguno de los dos y los metodos lo dicen. El
 * arrastre ({@link #setDragEnabled}) guarda la propiedad y nada mas, por lo mismo.
 */
public abstract class JTextComponent extends JComponent implements Scrollable, Accessible {

    /** La propiedad con el acelerador de foco. */
    public static final String FOCUS_ACCELERATOR_KEY = "focusAcceleratorKey";

    /** El nombre del mapa de teclas por omision. */
    public static final String DEFAULT_KEYMAP = "default";

    private static final Hashtable<String, Keymap> keymapTable =
            new Hashtable<String, Keymap>(17);

    private Document model;
    private transient Caret caret;
    private transient NavigationFilter navigationFilter;
    private transient Highlighter highlighter;
    private transient Keymap keymap;
    private transient CaretEvent caretEvent;
    private Color caretColor;
    private Color selectionColor;
    private Color selectedTextColor;
    private Color disabledTextColor;
    private boolean editable = true;
    private Insets margin;
    private char focusAccelerator;
    private boolean dragEnabled;
    private DropMode dropMode = DropMode.USE_SELECTION;
    private transient DropLocation dropLocation;

    /**
     * Un componente vacio, con su mapa de teclas y su cursor.
     *
     * <p><strong>El cursor lo pone el constructor y no el aspecto.</strong> En el JDK el cursor es
     * propiedad del aspecto: {@code BasicTextUI} le pasa uno al instalarse, y por eso el campo
     * nunca se queda en nulo. Esta biblioteca todavia no instala delegados de aspecto, asi que sin
     * esto {@code caret} quedaria nulo para siempre y cualquier {@code setCaretPosition} --que el
     * JDK tampoco protege-- reventaria con {@code NullPointerException}.
     *
     * <p>La diferencia observable es una sola y va en la direccion buena: {@link #getCaret} devuelve
     * un cursor en vez de nulo, que es lo mismo que devuelve el JDK apenas se le instala el aspecto.
     * Ponerle otro con {@link #setCaret} funciona igual.
     */
    public JTextComponent() {
        super();
        enableEvents(java.awt.AWTEvent.KEY_EVENT_MASK);
        caretEvent = null;
        setLayout(null);
        Keymap binding = getKeymap(DEFAULT_KEYMAP);
        if (binding == null) {
            binding = addKeymap(DEFAULT_KEYMAP, null);
        }
        setKeymap(binding);
        setCaret(new DefaultCaret());
    }

    public TextUI getUI() {
        return (TextUI) ui;
    }

    public void setUI(TextUI ui) {
        super.setUI(ui);
    }

    /**
     * Nada: no hay aspecto basico de texto en esta biblioteca.
     *
     * <p>{@code BasicTextUI} y sus derivados no estan, asi que un componente de texto se queda sin
     * aspecto salvo que le pongan uno con {@link #setUI}. Todo lo que no necesita el UI —el
     * documento, el cursor, los atributos— funciona igual.
     */
    public void updateUI() {
    }

    public void addCaretListener(CaretListener listener) {
        listenerList.add(CaretListener.class, listener);
    }

    public void removeCaretListener(CaretListener listener) {
        listenerList.remove(CaretListener.class, listener);
    }

    public CaretListener[] getCaretListeners() {
        return listenerList.getListeners(CaretListener.class);
    }

    /** Avisa que el cursor se movio; lo llama el cursor, no el componente. */
    protected void fireCaretUpdate(CaretEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == CaretListener.class) {
                ((CaretListener) listeners[i + 1]).caretUpdate(e);
            }
        }
    }

    /** Cambia el documento; el cursor y el resaltado se enganchan al nuevo. */
    public void setDocument(Document doc) {
        Document old = model;
        try {
            if (old instanceof AbstractDocument) {
                ((AbstractDocument) old).readLock();
            }
            if (accessibleContext != null) {
                model.removeDocumentListener(null);
            }
        } finally {
            if (old instanceof AbstractDocument) {
                ((AbstractDocument) old).readUnlock();
            }
        }
        model = doc;
        firePropertyChange("document", old, doc);
        revalidate();
        repaint();
    }

    public Document getDocument() {
        return model;
    }

    public void setComponentOrientation(ComponentOrientation o) {
        Document doc = getDocument();
        if (doc != null) {
            Object flag = ComponentOrientation.RIGHT_TO_LEFT;
            if (o.isLeftToRight()) {
                flag = ComponentOrientation.LEFT_TO_RIGHT;
            }
            doc.putProperty(TextAttribute.RUN_DIRECTION, flag);
        }
        super.setComponentOrientation(o);
    }

    /** Las acciones que este componente sabe hacer; las del juego de edicion. */
    public Action[] getActions() {
        return getUI().getEditorKit(this).getActions();
    }

    /** El margen entre el borde y el texto. */
    public void setMargin(Insets m) {
        Insets old = margin;
        margin = m;
        firePropertyChange("margin", old, m);
        invalidate();
    }

    public Insets getMargin() {
        return margin;
    }

    public void setNavigationFilter(NavigationFilter filter) {
        navigationFilter = filter;
    }

    public NavigationFilter getNavigationFilter() {
        return navigationFilter;
    }

    public Caret getCaret() {
        return caret;
    }

    /** Cambia el cursor; al viejo se le avisa que lo sacaron. */
    public void setCaret(Caret c) {
        if (caret != null) {
            caret.removeChangeListener(null);
            caret.deinstall(this);
        }
        Caret old = caret;
        caret = c;
        if (caret != null) {
            caret.install(this);
        }
        firePropertyChange("caret", old, caret);
    }

    public Highlighter getHighlighter() {
        return highlighter;
    }

    public void setHighlighter(Highlighter h) {
        if (highlighter != null) {
            highlighter.deinstall(this);
        }
        Highlighter old = highlighter;
        highlighter = h;
        if (highlighter != null) {
            highlighter.install(this);
        }
        firePropertyChange("highlighter", old, h);
    }

    public void setKeymap(Keymap map) {
        Keymap old = keymap;
        keymap = map;
        firePropertyChange("keymap", old, keymap);
        updateInputMap(old, map);
    }

    /** Guarda la propiedad; ver la nota de la clase sobre el arrastre. */
    public void setDragEnabled(boolean b) {
        dragEnabled = b;
    }

    public boolean getDragEnabled() {
        return dragEnabled;
    }

    public final void setDropMode(DropMode dropMode) {
        if (dropMode != null) {
            if (dropMode == DropMode.USE_SELECTION || dropMode == DropMode.INSERT) {
                this.dropMode = dropMode;
                return;
            }
        }
        throw new IllegalArgumentException(dropMode + ": Unsupported drop mode for text");
    }

    public final DropMode getDropMode() {
        return dropMode;
    }

    /** Donde caeria algo soltado en ese punto. */
    DropLocation dropLocationForPoint(Point p) {
        Position.Bias[] bias = new Position.Bias[1];
        int index = getUI().viewToModel(this, p, bias);
        if (bias[0] == null) {
            bias[0] = Position.Bias.Forward;
        }
        return new DropLocation(p, index, bias[0]);
    }

    /** Lo llama la maquinaria de arrastre, que en esta VM no corre. */
    Object setDropLocation(TransferHandler$DropLocation location, Object state,
            boolean forDrop) {
        return null;
    }

    public final DropLocation getDropLocation() {
        return dropLocation;
    }

    /**
     * Pasa el mapa de teclas a los mapas de entrada del componente.
     *
     * <p>No hace nada: {@code InputMap} y {@code ActionMap} no estan en esta biblioteca, y el
     * mapa de teclas se consulta directo. Esta el metodo porque el JDK lo llama desde
     * {@link #setKeymap} y una subclase podria redefinirlo.
     */
    void updateInputMap(Keymap oldKm, Keymap newKm) {
    }

    public Keymap getKeymap() {
        return keymap;
    }

    /** Agrega un mapa con ese nombre a la tabla compartida; ver la nota de la clase. */
    public static Keymap addKeymap(String nm, Keymap parent) {
        Keymap map = new DefaultKeymap(nm, parent);
        if (nm != null) {
            keymapTable.put(nm, map);
        }
        return map;
    }

    public static Keymap removeKeymap(String nm) {
        return keymapTable.remove(nm);
    }

    public static Keymap getKeymap(String nm) {
        return keymapTable.get(nm);
    }

    /**
     * Carga en un mapa las ataduras de tecla que nombran acciones.
     *
     * <p>Las ataduras nombran la accion por su nombre y no la traen: asi una tabla de atajos se
     * puede escribir sin tener las acciones a mano, y despues se resuelve contra las que el
     * componente ofrece.
     */
    public static void loadKeymap(Keymap map, KeyBinding[] bindings, Action[] actions) {
        Hashtable<String, Action> h = new Hashtable<String, Action>();
        for (int i = 0; i < actions.length; i++) {
            Action a = actions[i];
            String value = (String) a.getValue(Action.NAME);
            h.put((value != null ? value : ""), a);
        }
        for (int i = 0; i < bindings.length; i++) {
            Action a = h.get(bindings[i].actionName);
            if (a != null) {
                map.addActionForKeyStroke(bindings[i].key, a);
            }
        }
    }

    /** El componente de texto que tiene el foco; sin foco en esta VM, {@code null}. */
    static final JTextComponent getFocusedComponent() {
        return null;
    }

    public Color getCaretColor() {
        return caretColor;
    }

    public void setCaretColor(Color c) {
        Color old = caretColor;
        caretColor = c;
        firePropertyChange("caretColor", old, caretColor);
    }

    public Color getSelectionColor() {
        return selectionColor;
    }

    public void setSelectionColor(Color c) {
        Color old = selectionColor;
        selectionColor = c;
        firePropertyChange("selectionColor", old, selectionColor);
    }

    public Color getSelectedTextColor() {
        return selectedTextColor;
    }

    public void setSelectedTextColor(Color c) {
        Color old = selectedTextColor;
        selectedTextColor = c;
        firePropertyChange("selectedTextColor", old, selectedTextColor);
    }

    public Color getDisabledTextColor() {
        return disabledTextColor;
    }

    public void setDisabledTextColor(Color c) {
        Color old = disabledTextColor;
        disabledTextColor = c;
        firePropertyChange("disabledTextColor", old, disabledTextColor);
    }

    /**
     * Reemplaza lo seleccionado por ese texto.
     *
     * <p>Sin seleccion, inserta donde esta el cursor. Es la operacion que hace escribir una tecla,
     * y por eso esta aca y no en el documento: necesita saber del cursor.
     */
    public void replaceSelection(String content) {
        Document doc = getDocument();
        if (doc != null) {
            try {
                boolean composedTextSaved = false;
                int p0 = Math.min(caret.getDot(), caret.getMark());
                int p1 = Math.max(caret.getDot(), caret.getMark());
                if (doc instanceof AbstractDocument) {
                    ((AbstractDocument) doc).replace(p0, p1 - p0, content, null);
                } else {
                    if (p0 != p1) {
                        doc.remove(p0, p1 - p0);
                    }
                    if (content != null && content.length() > 0) {
                        doc.insertString(p0, content, null);
                    }
                }
            } catch (BadLocationException e) {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    public String getText(int offs, int len) throws BadLocationException {
        return getDocument().getText(offs, len);
    }

    /** @deprecated es {@link #modelToView2D}. */
    @Deprecated
    public Rectangle modelToView(int pos) throws BadLocationException {
        return getUI().modelToView(this, pos);
    }

    /** Donde cae esa posicion en la pantalla. */
    public Rectangle2D modelToView2D(int pos) throws BadLocationException {
        return getUI().modelToView2D(this, pos, Position.Bias.Forward);
    }

    /** @deprecated es {@link #viewToModel2D}. */
    @Deprecated
    public int viewToModel(Point pt) {
        return getUI().viewToModel(this, pt);
    }

    /** Que posicion del texto hay en ese punto. */
    public int viewToModel2D(Point2D pt) {
        return getUI().viewToModel2D(this, pt, new Position.Bias[1]);
    }

    /** No hay portapapeles del sistema en esta VM; ver la nota de la clase. */
    public void cut() {
        throw new UnsupportedOperationException("esta VM no tiene portapapeles del sistema");
    }

    /** No hay portapapeles del sistema en esta VM; ver la nota de la clase. */
    public void copy() {
        throw new UnsupportedOperationException("esta VM no tiene portapapeles del sistema");
    }

    /** No hay portapapeles del sistema en esta VM; ver la nota de la clase. */
    public void paste() {
        throw new UnsupportedOperationException("esta VM no tiene portapapeles del sistema");
    }

    /** Mueve el cursor extendiendo la seleccion. */
    public void moveCaretPosition(int pos) {
        Document doc = getDocument();
        if (doc != null) {
            if (pos > doc.getLength() || pos < 0) {
                throw new IllegalArgumentException("bad position: " + pos);
            }
            caret.moveDot(pos);
        }
    }

    /** La tecla que le da el foco a este componente con Alt. */
    public void setFocusAccelerator(char aKey) {
        aKey = Character.toUpperCase(aKey);
        char old = focusAccelerator;
        focusAccelerator = aKey;
        firePropertyChange(FOCUS_ACCELERATOR_KEY, old, focusAccelerator);
    }

    public char getFocusAccelerator() {
        return focusAccelerator;
    }

    /** Lee un documento del flujo con el juego de edicion instalado. */
    public void read(Reader in, Object desc) throws IOException {
        EditorKit kit = getUI().getEditorKit(this);
        Document doc = kit.createDefaultDocument();
        if (desc != null) {
            doc.putProperty(Document.StreamDescriptionProperty, desc);
        }
        try {
            kit.read(in, doc, 0);
            setDocument(doc);
        } catch (BadLocationException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void write(Writer out) throws IOException {
        Document doc = getDocument();
        try {
            getUI().getEditorKit(this).write(out, doc, 0, doc.getLength());
        } catch (BadLocationException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void removeNotify() {
        super.removeNotify();
    }

    /** Mueve el cursor y deshace la seleccion. */
    public void setCaretPosition(int position) {
        Document doc = getDocument();
        if (doc != null) {
            if (position > doc.getLength() || position < 0) {
                throw new IllegalArgumentException("bad position: " + position);
            }
            caret.setDot(position);
        }
    }

    public int getCaretPosition() {
        return caret.getDot();
    }

    /** Reemplaza todo el texto. */
    public void setText(String t) {
        try {
            Document doc = getDocument();
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument) doc).replace(0, doc.getLength(), t, null);
            } else {
                doc.remove(0, doc.getLength());
                doc.insertString(0, t, null);
            }
        } catch (BadLocationException e) {
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    public String getText() {
        Document doc = getDocument();
        String txt;
        try {
            txt = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            txt = null;
        }
        return txt;
    }

    /** Lo seleccionado, o {@code null} si no hay seleccion. */
    public String getSelectedText() {
        String txt = null;
        int p0 = Math.min(caret.getDot(), caret.getMark());
        int p1 = Math.max(caret.getDot(), caret.getMark());
        if (p0 != p1) {
            try {
                Document doc = getDocument();
                txt = doc.getText(p0, p1 - p0);
            } catch (BadLocationException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        return txt;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean b) {
        if (b != editable) {
            boolean oldVal = editable;
            editable = b;
            firePropertyChange("editable", Boolean.valueOf(oldVal), Boolean.valueOf(editable));
            repaint();
        }
    }

    public int getSelectionStart() {
        int start = Math.min(caret.getDot(), caret.getMark());
        return start;
    }

    public void setSelectionStart(int selectionStart) {
        select(selectionStart, getSelectionEnd());
    }

    public int getSelectionEnd() {
        int end = Math.max(caret.getDot(), caret.getMark());
        return end;
    }

    public void setSelectionEnd(int selectionEnd) {
        select(getSelectionStart(), selectionEnd);
    }

    /** Selecciona ese tramo; los limites se acomodan al documento. */
    public void select(int selectionStart, int selectionEnd) {
        int docLength = getDocument().getLength();

        if (selectionStart < 0) {
            selectionStart = 0;
        }
        if (selectionStart > docLength) {
            selectionStart = docLength;
        }
        if (selectionEnd > docLength) {
            selectionEnd = docLength;
        }
        if (selectionEnd < selectionStart) {
            selectionEnd = selectionStart;
        }

        setCaretPosition(selectionStart);
        moveCaretPosition(selectionEnd);
    }

    public void selectAll() {
        Document doc = getDocument();
        if (doc != null) {
            setCaretPosition(0);
            moveCaretPosition(doc.getLength());
        }
    }

    public String getToolTipText(MouseEvent event) {
        String retValue = super.getToolTipText(event);
        if (retValue == null) {
            TextUI ui = getUI();
            if (ui != null) {
                retValue = ui.getToolTipText2D(this,
                        new java.awt.geom.Point2D$Float(event.getX(), event.getY()));
            }
        }
        return retValue;
    }

    // -- Scrollable ------------------------------------------------------------------------------

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    /**
     * Cuanto avanza un paso chico: el alto de una linea, o el ancho de un caracter.
     *
     * <p>Se mide de verdad —cuanto ocupa la linea visible— y no con un numero fijo: en un texto
     * con lineas de distinto alto, un numero fijo dejaria el texto cortado a la mitad.
     */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return visibleRect.height / 10;
        }
        if (orientation == SwingConstants.HORIZONTAL) {
            return visibleRect.width / 10;
        }
        throw new IllegalArgumentException("Invalid orientation: " + orientation);
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return visibleRect.height;
        }
        if (orientation == SwingConstants.HORIZONTAL) {
            return visibleRect.width;
        }
        throw new IllegalArgumentException("Invalid orientation: " + orientation);
    }

    /** Sigue al ancho de la ventana si el padre es mas ancho que el texto. */
    public boolean getScrollableTracksViewportWidth() {
        java.awt.Container parent = getParent();
        if (parent instanceof javax.swing.JViewport) {
            return parent.getWidth() > getPreferredSize().width;
        }
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        java.awt.Container parent = getParent();
        if (parent instanceof javax.swing.JViewport) {
            return parent.getHeight() > getPreferredSize().height;
        }
        return false;
    }

    // -- impresion -------------------------------------------------------------------------------

    /** No hay impresora en esta VM; ver la nota de la clase. */
    public boolean print() throws PrinterException {
        throw new PrinterException("esta VM no tiene impresion");
    }

    /** No hay impresora en esta VM; ver la nota de la clase. */
    public boolean print(MessageFormat headerFormat, MessageFormat footerFormat)
            throws PrinterException {
        throw new PrinterException("esta VM no tiene impresion");
    }

    /** No hay impresora en esta VM; ver la nota de la clase. */
    public boolean print(MessageFormat headerFormat, MessageFormat footerFormat,
            boolean showPrintDialog, PrintService service, PrintRequestAttributeSet attributes,
            boolean interactive) throws PrinterException {
        throw new PrinterException("esta VM no tiene impresion");
    }

    /** No hay impresion en esta VM; ver la nota de la clase. */
    public Printable getPrintable(MessageFormat headerFormat, MessageFormat footerFormat) {
        throw new UnsupportedOperationException("esta VM no tiene impresion");
    }

    /** Sin contexto de accesibilidad: no hay tecnologia asistiva en esta VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }

    protected String paramString() {
        String editableString = (editable ? "true" : "false");
        String caretColorString = (caretColor != null ? caretColor.toString() : "");
        String selectionColorString = (selectionColor != null ? selectionColor.toString() : "");
        String disabledTextColorString = (disabledTextColor != null
                ? disabledTextColor.toString() : "");
        String selectedTextColorString = (selectedTextColor != null
                ? selectedTextColor.toString() : "");
        String marginString = (margin != null ? margin.toString() : "");

        return super.paramString() + ",caretColor=" + caretColorString + ",disabledTextColor="
                + disabledTextColorString + ",editable=" + editableString + ",margin="
                + marginString + ",selectedTextColor=" + selectedTextColorString
                + ",selectionColor=" + selectionColorString;
    }

    /** Los metodos de entrada —escritura oriental— no estan en esta VM. */
    protected void processInputMethodEvent(InputMethodEvent e) {
        super.processInputMethodEvent(e);
    }

    /** {@code null}: sin metodos de entrada; ver arriba. */
    public InputMethodRequests getInputMethodRequests() {
        return null;
    }

    public void addInputMethodListener(InputMethodListener l) {
        super.addInputMethodListener(l);
    }

    /** No hay texto en composicion sin metodos de entrada. */
    protected boolean saveComposedText(int pos) {
        return false;
    }

    protected void restoreComposedText() {
    }

    boolean composedTextExists() {
        return false;
    }

    /**
     * Una tecla atada al nombre de una accion.
     *
     * <p>Nombra la accion en vez de traerla; ver {@link JTextComponent#loadKeymap}.
     */
    public static class KeyBinding {

        public KeyStroke key;
        public String actionName;

        public KeyBinding(KeyStroke key, String actionName) {
            this.key = key;
            this.actionName = actionName;
        }
    }

    /** Donde caeria lo que se esta arrastrando: una posicion del texto, con su sentido. */
    public static final class DropLocation extends TransferHandler$DropLocation {

        private final int index;
        private final Position.Bias bias;

        DropLocation(Point p, int index, Position.Bias bias) {
            super(p);
            this.index = index;
            this.bias = bias;
        }

        public int getIndex() {
            return index;
        }

        public Position.Bias getBias() {
            return bias;
        }

        public String toString() {
            return getClass().getName() + "[dropPoint=" + getDropPoint() + ",index=" + index
                    + ",bias=" + bias + "]";
        }
    }

    /**
     * El mapa de teclas de siempre: una tabla con padre de resolucion.
     *
     * <p>Es privado en el JDK y aca tambien: se llega a el por {@link JTextComponent#addKeymap}.
     */
    static class DefaultKeymap implements Keymap {

        private String nm;
        private Keymap parent;
        private Hashtable<KeyStroke, Action> bindings;
        private Action defaultAction;

        DefaultKeymap(String nm, Keymap parent) {
            this.nm = nm;
            this.parent = parent;
            bindings = new Hashtable<KeyStroke, Action>();
        }

        public String getName() {
            return nm;
        }

        public Action getDefaultAction() {
            if (defaultAction != null) {
                return defaultAction;
            }
            return (parent != null) ? parent.getDefaultAction() : null;
        }

        public void setDefaultAction(Action a) {
            defaultAction = a;
        }

        public Action getAction(KeyStroke key) {
            Action a = bindings.get(key);
            if ((a == null) && (parent != null)) {
                a = parent.getAction(key);
            }
            return a;
        }

        public KeyStroke[] getBoundKeyStrokes() {
            KeyStroke[] keys = new KeyStroke[bindings.size()];
            int i = 0;
            for (java.util.Enumeration<KeyStroke> e = bindings.keys(); e.hasMoreElements();) {
                keys[i] = e.nextElement();
                i = i + 1;
            }
            return keys;
        }

        public Action[] getBoundActions() {
            Action[] actions = new Action[bindings.size()];
            int i = 0;
            for (java.util.Enumeration<Action> e = bindings.elements(); e.hasMoreElements();) {
                actions[i] = e.nextElement();
                i = i + 1;
            }
            return actions;
        }

        public KeyStroke[] getKeyStrokesForAction(Action a) {
            if (a == null) {
                return null;
            }
            Vector<KeyStroke> v = new Vector<KeyStroke>();
            for (java.util.Enumeration<KeyStroke> e = bindings.keys(); e.hasMoreElements();) {
                KeyStroke k = e.nextElement();
                if (bindings.get(k) == a) {
                    v.addElement(k);
                }
            }
            if (v.size() == 0) {
                return null;
            }
            KeyStroke[] keys = new KeyStroke[v.size()];
            v.copyInto(keys);
            return keys;
        }

        public boolean isLocallyDefined(KeyStroke key) {
            return bindings.containsKey(key);
        }

        public void addActionForKeyStroke(KeyStroke key, Action a) {
            bindings.put(key, a);
        }

        public void removeKeyStrokeBinding(KeyStroke key) {
            bindings.remove(key);
        }

        public void removeBindings() {
            bindings.clear();
        }

        public Keymap getResolveParent() {
            return parent;
        }

        public void setResolveParent(Keymap parent) {
            this.parent = parent;
        }

        public String toString() {
            return "Keymap[" + nm + "]" + bindings;
        }
    }
}
