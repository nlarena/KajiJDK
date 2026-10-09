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
import java.awt.font.TextAttribute;
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

import javax.accessibility.Accessible;
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
 * The base of every component that shows or edits text.
 *
 * <h2>Four pieces and none of them is this class</h2>
 *
 * <p>The component does not keep the text, nor know how to draw it, nor know where the cursor
 * is. It keeps references to the four that do know:
 *
 * <ul>
 * <li>The {@link Document}, which has the text and the structure.
 * <li>The {@link Caret}, which knows where typing happens and what is selected.
 * <li>The {@link Highlighter}, which paints the backgrounds.
 * <li>The {@link TextUI}, which has the view tree and translates between text and screen.
 * </ul>
 *
 * <p>Almost every method here is that: asking one of the four. {@code setText} is a
 * {@code remove} plus an {@code insertString} on the document; {@code modelToView} is a call to
 * the UI; {@code getSelectionStart} is the smaller of the cursor's dot and mark. That the class
 * is so big and so thin at the same time is intentional: it is the facade.
 *
 * <h2>The key maps</h2>
 *
 * <p>The {@link Keymap}s are kept in a static table shared by name. It is one of the few global
 * things left in Swing, and it comes from before {@code InputMap}: an editor could name its map
 * and another ask for it by that name.
 *
 * <h2>What is not there</h2>
 *
 * <p>{@link #cut}, {@link #copy} and {@link #paste} need a system clipboard, and {@link #print}
 * a printer: this VM has neither of the two and the methods say so. Dragging
 * ({@link #setDragEnabled}) keeps the property and nothing else, for the same reason.
 */
public abstract class JTextComponent extends JComponent implements Scrollable, Accessible {

    /** The property with the focus accelerator. */
    public static final String FOCUS_ACCELERATOR_KEY = "focusAcceleratorKey";

    /** The name of the default key map. */
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
     * An empty component, with its key map and its cursor.
     *
     * <p><strong>The cursor is set by the constructor and not by the look and feel.</strong> In the
     * JDK the cursor is the look and feel's property: {@code BasicTextUI} passes one on installing,
     * and that is why the field is never left null. This library does not install look and feel
     * delegates yet, so without this {@code caret} would be left null for ever and any
     * {@code setCaretPosition} --which the JDK does not protect either-- would blow up with
     * {@code NullPointerException}.
     *
     * <p>The observable difference is a single one and it goes the good way: {@link #getCaret}
     * returns a cursor instead of null, which is the same as the JDK returns as soon as the look
     * and feel is installed on it. Setting another with {@link #setCaret} works the same.
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
     * Nothing: there is no basic text look and feel in this library.
     *
     * <p>{@code BasicTextUI} and its derivatives are not there, so a text component is left with no
     * look and feel unless one is set on it with {@link #setUI}. Everything that does not need the
     * UI --the document, the cursor, the attributes-- works the same.
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

    /** It reports that the cursor moved; the cursor calls it, not the component. */
    protected void fireCaretUpdate(CaretEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == CaretListener.class) {
                ((CaretListener) listeners[i + 1]).caretUpdate(e);
            }
        }
    }

    /** It changes the document; the cursor and the highlight hook themselves to the new one. */
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

    /** The actions this component knows how to do; the editor kit's. */
    public Action[] getActions() {
        return getUI().getEditorKit(this).getActions();
    }

    /** The margin between the border and the text. */
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

    /** It changes the cursor; the old one is told it was removed. */
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

    /** It keeps the property; see the class note about dragging. */
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

    /** Where something dropped at that point would fall. */
    DropLocation dropLocationForPoint(Point p) {
        Position.Bias[] bias = new Position.Bias[1];
        int index = getUI().viewToModel(this, p, bias);
        if (bias[0] == null) {
            bias[0] = Position.Bias.Forward;
        }
        return new DropLocation(p, index, bias[0]);
    }

    /** The dragging machinery calls it, which in this VM does not run. */
    Object setDropLocation(TransferHandler$DropLocation location, Object state,
            boolean forDrop) {
        return null;
    }

    public final DropLocation getDropLocation() {
        return dropLocation;
    }

    /**
     * It passes the key map to the component's input maps.
     *
     * <p>It does nothing: {@code InputMap} and {@code ActionMap} are not in this library, and the
     * key map is consulted directly. The method is there because the JDK calls it from
     * {@link #setKeymap} and a subclass could redefine it.
     */
    void updateInputMap(Keymap oldKm, Keymap newKm) {
    }

    public Keymap getKeymap() {
        return keymap;
    }

    /** It adds a map with that name to the shared table; see the class note. */
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
     * It loads into a map the key bindings that name actions.
     *
     * <p>The bindings name the action by its name and do not bring it: that way a shortcut table
     * can be written without having the actions at hand, and afterwards it is resolved against
     * those the component offers.
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

    /** The text component that has the focus; with no focus in this VM, {@code null}. */
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
     * It replaces what is selected by that text.
     *
     * <p>With no selection, it inserts where the cursor is. It is the operation typing a key does,
     * and that is why it is here and not in the document: it needs to know about the cursor.
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

    /** @deprecated it is {@link #modelToView2D}. */
    @Deprecated
    public Rectangle modelToView(int pos) throws BadLocationException {
        return getUI().modelToView(this, pos);
    }

    /** Where that position falls on the screen. */
    public Rectangle2D modelToView2D(int pos) throws BadLocationException {
        return getUI().modelToView2D(this, pos, Position.Bias.Forward);
    }

    /** @deprecated it is {@link #viewToModel2D}. */
    @Deprecated
    public int viewToModel(Point pt) {
        return getUI().viewToModel(this, pt);
    }

    /** Which position of the text is at that point. */
    public int viewToModel2D(Point2D pt) {
        return getUI().viewToModel2D(this, pt, new Position.Bias[1]);
    }

    /** There is no system clipboard in this VM; see the class note. */
    public void cut() {
        throw new UnsupportedOperationException("this VM has no system clipboard");
    }

    /** There is no system clipboard in this VM; see the class note. */
    public void copy() {
        throw new UnsupportedOperationException("this VM has no system clipboard");
    }

    /** There is no system clipboard in this VM; see the class note. */
    public void paste() {
        throw new UnsupportedOperationException("this VM has no system clipboard");
    }

    /** It moves the cursor extending the selection. */
    public void moveCaretPosition(int pos) {
        Document doc = getDocument();
        if (doc != null) {
            if (pos > doc.getLength() || pos < 0) {
                throw new IllegalArgumentException("bad position: " + pos);
            }
            caret.moveDot(pos);
        }
    }

    /** The key that gives this component the focus with Alt. */
    public void setFocusAccelerator(char aKey) {
        aKey = Character.toUpperCase(aKey);
        char old = focusAccelerator;
        focusAccelerator = aKey;
        firePropertyChange(FOCUS_ACCELERATOR_KEY, old, focusAccelerator);
    }

    public char getFocusAccelerator() {
        return focusAccelerator;
    }

    /** It reads a document from the stream with the installed editor kit. */
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

    /** It moves the cursor and undoes the selection. */
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

    /** It replaces all the text. */
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

    /** What is selected, or {@code null} if there is no selection. */
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

    /** It selects that stretch; the bounds are fitted to the document. */
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
     * How much one small step advances: a line's height, or a character's width.
     *
     * <p>It is really measured --how much the visible line takes up-- and not with a fixed number:
     * in a text with lines of different heights, a fixed number would leave the text cut in half.
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

    /** It follows the viewport's width if the parent is wider than the text. */
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

    // -- printing ---------------------------------------------------------------------------------

    /** There is no printer in this VM; see the class note. */
    public boolean print() throws PrinterException {
        throw new PrinterException("this VM has no printing");
    }

    /** There is no printer in this VM; see the class note. */
    public boolean print(MessageFormat headerFormat, MessageFormat footerFormat)
            throws PrinterException {
        throw new PrinterException("this VM has no printing");
    }

    /** There is no printer in this VM; see the class note. */
    public boolean print(MessageFormat headerFormat, MessageFormat footerFormat,
            boolean showPrintDialog, PrintService service, PrintRequestAttributeSet attributes,
            boolean interactive) throws PrinterException {
        throw new PrinterException("this VM has no printing");
    }

    /** There is no printing in this VM; see the class note. */
    public Printable getPrintable(MessageFormat headerFormat, MessageFormat footerFormat) {
        throw new UnsupportedOperationException("this VM has no printing");
    }

    /** No accessibility context: there is no assistive technology in this VM. */
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

    /** The input methods --eastern writing-- are not in this VM. */
    protected void processInputMethodEvent(InputMethodEvent e) {
        super.processInputMethodEvent(e);
    }

    /** {@code null}: with no input methods; see above. */
    public InputMethodRequests getInputMethodRequests() {
        return null;
    }

    public void addInputMethodListener(InputMethodListener l) {
        super.addInputMethodListener(l);
    }

    /** There is no text being composed without input methods. */
    protected boolean saveComposedText(int pos) {
        return false;
    }

    protected void restoreComposedText() {
    }

    boolean composedTextExists() {
        return false;
    }

    /**
     * A key tied to an action's name.
     *
     * <p>It names the action instead of bringing it; see {@link JTextComponent#loadKeymap}.
     */
    public static class KeyBinding {

        public KeyStroke key;
        public String actionName;

        public KeyBinding(KeyStroke key, String actionName) {
            this.key = key;
            this.actionName = actionName;
        }
    }

    /** Where what is being dragged would fall: a position in the text, with its bias. */
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
     * The usual key map: a table with a resolving parent.
     *
     * <p>It is private in the JDK and here too: it is reached through
     * {@link JTextComponent#addKeymap}.
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
