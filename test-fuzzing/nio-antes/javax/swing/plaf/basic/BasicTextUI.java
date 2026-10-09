package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TextUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * The look and feel common to every text component.
 *
 * <h2>What it does, in one line</h2>
 *
 * <p>It builds a tree of views over the document and draws it. Everything else -- the caret,
 * the highlighting, the keys -- it delegates to objects that can be replaced.
 *
 * <h2>The root view</h2>
 *
 * <p>Between the component and the document's view there is one view more, the
 * {@link RootView}. It is not decorative: the document's view is changed by the editor kit
 * every time the document changes, and something has to stay fixed so that the component always
 * has somebody to ask. The root is that fixed point. It is also the one that translates between
 * the component's coordinates, which have margins, and the views', which do not.
 *
 * <p>The root also acts as a factory: when the view below asks for a view for an element, the
 * root asks the editor kit first and, if it does not have one, the look and feel. That is why
 * {@code BasicTextUI} implements {@link ViewFactory}.
 *
 * <h2>What is drawn and what is not</h2>
 *
 * <p>With no window there is nothing to draw, but the arithmetic is done all the same: the tree
 * of views is built, laid out and answers {@link #modelToView} and {@link #viewToModel}
 * exactly the same. The only thing missing is the {@link Graphics}' addressee.
 */
public abstract class BasicTextUI extends TextUI implements ViewFactory {

    private JTextComponent editor;
    private RootView rootView;
    private boolean painted;
    private Handler handler;
    private transient boolean creatingUI;

    /** A look and feel with no component; it is completed in {@link #installUI}. */
    public BasicTextUI() {
        painted = false;
    }

    /** The caret that is set if the component does not bring one of its own. */
    protected Caret createCaret() {
        return new BasicCaret();
    }

    /** The highlighter that is set if the component does not bring one of its own. */
    protected Highlighter createHighlighter() {
        return new BasicHighlighter();
    }

    /** The name of the key map shared by the components of this type. */
    protected String getKeymapName() {
        String nm = getClass().getName();
        int index = nm.lastIndexOf('.');
        if (index >= 0) {
            nm = nm.substring(index + 1, nm.length());
        }
        return nm;
    }

    /**
     * It builds the key map.
     *
     * <p>It is shared between every component of the same type: building one per component would
     * multiply the same table by each text field on the screen.
     */
    protected Keymap createKeymap() {
        String nm = getKeymapName();
        Keymap map = JTextComponent.getKeymap(nm);
        if (map == null) {
            Keymap parent = JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
            map = JTextComponent.addKeymap(nm, parent);
        }
        return map;
    }

    /** It reacts to the component's changes that force the views to be rebuilt. */
    protected void propertyChange(PropertyChangeEvent evt) {
    }

    /** The prefix the look and feel's values are looked up with, for instance {@code TextField}. */
    protected abstract String getPropertyPrefix();

    private static final javax.swing.plaf.FontUIResource FONT =
            new javax.swing.plaf.FontUIResource("Dialog", java.awt.Font.PLAIN, 12);
    private static final javax.swing.plaf.ColorUIResource BACKGROUND =
            new javax.swing.plaf.ColorUIResource(255, 255, 255);
    private static final javax.swing.plaf.ColorUIResource FOREGROUND =
            new javax.swing.plaf.ColorUIResource(51, 51, 51);
    private static final javax.swing.plaf.ColorUIResource SELECTION =
            new javax.swing.plaf.ColorUIResource(184, 207, 229);

    /**
     * The margin that falls to this component.
     *
     * <p>Zero in the fields and in the area, three in the two editor panes. The difference makes
     * sense: an editor pane shows a document and text stuck to the edge reads badly; a one-line
     * field already comes with the air its border gives it. Measured in Metal (JDK 25).
     */
    private java.awt.Insets defaultMargin() {
        String prefix = getPropertyPrefix();
        if ("EditorPane".equals(prefix) || "TextPane".equals(prefix)) {
            return new javax.swing.plaf.InsetsUIResource(3, 3, 3, 3);
        }
        return new javax.swing.plaf.InsetsUIResource(0, 0, 0, 0);
    }

    /**
     * It sets colours, typeface and margins.
     *
     * <p>Only where the component does not bring a value of its own: if it overwrote a colour set
     * by hand, changing the look and feel would erase what the program configured.
     */
    protected void installDefaults() {
        if (editor.getFont() == null || editor.getFont() instanceof UIResource) {
            editor.setFont(FONT);
        }
        if (editor.getBackground() == null || editor.getBackground() instanceof UIResource) {
            editor.setBackground(BACKGROUND);
        }
        if (editor.getForeground() == null || editor.getForeground() instanceof UIResource) {
            editor.setForeground(FOREGROUND);
        }
        if (editor.getCaretColor() == null || editor.getCaretColor() instanceof UIResource) {
            editor.setCaretColor(FOREGROUND);
        }
        if (editor.getSelectionColor() == null
                || editor.getSelectionColor() instanceof UIResource) {
            editor.setSelectionColor(SELECTION);
        }
        if (editor.getSelectedTextColor() == null
                || editor.getSelectedTextColor() instanceof UIResource) {
            editor.setSelectedTextColor(FOREGROUND);
        }
        if (editor.getDisabledTextColor() == null
                || editor.getDisabledTextColor() instanceof UIResource) {
            editor.setDisabledTextColor(SELECTION);
        }
        if (editor.getMargin() == null || editor.getMargin() instanceof UIResource) {
            editor.setMargin(defaultMargin());
        }
        Caret caret = editor.getCaret();
        if (caret == null || caret instanceof UIResource) {
            caret = createCaret();
            editor.setCaret(caret);
            caret.setBlinkRate(500);
        }
        Highlighter highlighter = editor.getHighlighter();
        if (highlighter == null || highlighter instanceof UIResource) {
            editor.setHighlighter(createHighlighter());
        }
    }

    protected void uninstallDefaults() {
    }

    protected void installListeners() {
    }

    protected void uninstallListeners() {
    }

    protected void installKeyboardActions() {
        editor.setKeymap(createKeymap());
    }

    protected void uninstallKeyboardActions() {
        editor.setKeymap(null);
    }

    /** It fills the background with the component's colour. */
    protected void paintBackground(Graphics g) {
        g.setColor(editor.getBackground());
        g.fillRect(0, 0, editor.getWidth(), editor.getHeight());
    }

    protected final JTextComponent getComponent() {
        return editor;
    }

    /** It rebuilds the tree of views; it is called when the document changes. */
    protected void modelChanged() {
        ViewFactory f = rootView.getViewFactory();
        Document doc = editor.getDocument();
        if (doc != null && f != null) {
            Element elem = doc.getDefaultRootElement();
            setView(f.create(elem));
        }
    }

    /** It hangs that view from the root. */
    protected final void setView(View v) {
        rootView.setView(v);
        painted = false;
        editor.revalidate();
        editor.repaint();
    }

    /**
     * It draws the text, the highlighting and the caret, in that order.
     *
     * <p>The order matters: the highlighting goes below the text so as not to cover it, and the
     * caret above everything so that it is seen over the selection.
     */
    protected void paintSafely(Graphics g) {
        painted = true;
        Highlighter highlighter = editor.getHighlighter();
        Caret caret = editor.getCaret();

        if (editor.isOpaque()) {
            paintBackground(g);
        }
        if (highlighter != null) {
            highlighter.paint(g);
        }
        Rectangle alloc = getVisibleEditorRect();
        if (alloc != null) {
            rootView.paint(g, alloc);
        }
        if (caret != null) {
            caret.paint(g);
        }
    }

    public void installUI(JComponent c) {
        if (!(c instanceof JTextComponent)) {
            throw new Error("TextUI needs JTextComponent");
        }
        editor = (JTextComponent) c;
        painted = false;
        rootView = new RootView(this);

        // The document goes first: installing the caret with no document would leave it with
        // nowhere to stand.
        Document doc = editor.getDocument();
        if (doc == null) {
            editor.setDocument(getEditorKit(editor).createDefaultDocument());
        } else {
            modelChanged();
        }
        installDefaults();
        installListeners();
        installKeyboardActions();

        if (handler == null) {
            handler = new Handler(this);
        }
        editor.addPropertyChangeListener(handler);

        Caret caret = editor.getCaret();
        if (caret != null) {
            caret.install(editor);
        }
        Highlighter h = editor.getHighlighter();
        if (h != null) {
            h.install(editor);
        }
    }

    public void uninstallUI(JComponent c) {
        Caret caret = editor.getCaret();
        if (caret != null) {
            caret.deinstall(editor);
        }
        Highlighter h = editor.getHighlighter();
        if (h != null) {
            h.deinstall(editor);
        }
        if (handler != null) {
            editor.removePropertyChangeListener(handler);
        }
        rootView.setView(null);
        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
        editor = null;
    }

    public void update(Graphics g, JComponent c) {
        paint(g, c);
    }

    /**
     * It draws the component.
     *
     * <p>It is final: whatever a subclass wants to change goes in {@link #paintSafely}, which runs
     * with the document taken for reading. Drawing without that lock could read a half-changed
     * document.
     */
    public final void paint(Graphics g, JComponent c) {
        if ((rootView.getViewCount() > 0) && (rootView.getView(0) != null)) {
            Document doc = editor.getDocument();
            if (doc instanceof javax.swing.text.AbstractDocument) {
                ((javax.swing.text.AbstractDocument) doc).readLock();
            }
            try {
                paintSafely(g);
            } finally {
                if (doc instanceof javax.swing.text.AbstractDocument) {
                    ((javax.swing.text.AbstractDocument) doc).readUnlock();
                }
            }
        }
    }

    public Dimension getPreferredSize(JComponent c) {
        Document doc = editor.getDocument();
        Insets i = c.getInsets();
        Dimension d = c.getSize();

        if (doc instanceof javax.swing.text.AbstractDocument) {
            ((javax.swing.text.AbstractDocument) doc).readLock();
        }
        try {
            if ((d.width > (i.left + i.right)) && (d.height > (i.top + i.bottom))) {
                rootView.setSize(d.width - i.left - i.right, d.height - i.top - i.bottom);
            } else if (d.width == 0 && d.height == 0) {
                // Still with no size: it is let grow so that the view says how much it wants.
                rootView.setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
            d.width = (int) Math.min((long) rootView.getPreferredSpan(View.X_AXIS)
                    + (long) i.left + (long) i.right, Integer.MAX_VALUE);
            d.height = (int) Math.min((long) rootView.getPreferredSpan(View.Y_AXIS)
                    + (long) i.top + (long) i.bottom, Integer.MAX_VALUE);
        } finally {
            if (doc instanceof javax.swing.text.AbstractDocument) {
                ((javax.swing.text.AbstractDocument) doc).readUnlock();
            }
        }
        return d;
    }

    public Dimension getMinimumSize(JComponent c) {
        Document doc = editor.getDocument();
        Insets i = c.getInsets();
        Dimension d = new Dimension();
        if (doc instanceof javax.swing.text.AbstractDocument) {
            ((javax.swing.text.AbstractDocument) doc).readLock();
        }
        try {
            d.width = (int) rootView.getMinimumSpan(View.X_AXIS) + i.left + i.right;
            d.height = (int) rootView.getMinimumSpan(View.Y_AXIS) + i.top + i.bottom;
        } finally {
            if (doc instanceof javax.swing.text.AbstractDocument) {
                ((javax.swing.text.AbstractDocument) doc).readUnlock();
            }
        }
        return d;
    }

    public Dimension getMaximumSize(JComponent c) {
        Document doc = editor.getDocument();
        Insets i = c.getInsets();
        Dimension d = new Dimension();
        if (doc instanceof javax.swing.text.AbstractDocument) {
            ((javax.swing.text.AbstractDocument) doc).readLock();
        }
        try {
            d.width = (int) Math.min((long) rootView.getMaximumSpan(View.X_AXIS)
                    + (long) i.left + (long) i.right, Integer.MAX_VALUE);
            d.height = (int) Math.min((long) rootView.getMaximumSpan(View.Y_AXIS)
                    + (long) i.top + (long) i.bottom, Integer.MAX_VALUE);
        } finally {
            if (doc instanceof javax.swing.text.AbstractDocument) {
                ((javax.swing.text.AbstractDocument) doc).readUnlock();
            }
        }
        return d;
    }

    /** The rectangle the text goes in: the component minus the margins. */
    protected Rectangle getVisibleEditorRect() {
        Rectangle alloc = editor.getBounds();
        if ((alloc.width > 0) && (alloc.height > 0)) {
            alloc.x = 0;
            alloc.y = 0;
            Insets insets = editor.getInsets();
            alloc.x = alloc.x + insets.left;
            alloc.y = alloc.y + insets.top;
            alloc.width = alloc.width - insets.left - insets.right;
            alloc.height = alloc.height - insets.top - insets.bottom;
            return alloc;
        }
        return null;
    }

    public Rectangle modelToView(JTextComponent tc, int pos) throws BadLocationException {
        return modelToView(tc, pos, Position.Bias.Forward);
    }

    public Rectangle modelToView(JTextComponent tc, int pos, Position.Bias bias)
            throws BadLocationException {
        Rectangle2D r = modelToView2D(tc, pos, bias);
        return (r == null) ? null : r.getBounds();
    }

    /** Where that position of the document falls on the screen. */
    public Rectangle2D modelToView2D(JTextComponent tc, int pos, Position.Bias bias)
            throws BadLocationException {
        Document doc = editor.getDocument();
        if (doc instanceof javax.swing.text.AbstractDocument) {
            ((javax.swing.text.AbstractDocument) doc).readLock();
        }
        try {
            Rectangle alloc = getVisibleEditorRect();
            if (alloc != null) {
                rootView.setSize(alloc.width, alloc.height);
                Shape s = rootView.modelToView(pos, alloc, bias);
                if (s != null) {
                    return s.getBounds();
                }
            }
        } finally {
            if (doc instanceof javax.swing.text.AbstractDocument) {
                ((javax.swing.text.AbstractDocument) doc).readUnlock();
            }
        }
        return null;
    }

    public int viewToModel(JTextComponent tc, Point pt) {
        return viewToModel(tc, pt, new Position.Bias[1]);
    }

    public int viewToModel(JTextComponent tc, Point pt, Position.Bias[] biasReturn) {
        return viewToModel2D(tc, pt, biasReturn);
    }

    /** Which position of the document falls at that point of the screen. */
    public int viewToModel2D(JTextComponent tc, Point2D pt, Position.Bias[] biasReturn) {
        int offs = -1;
        Document doc = editor.getDocument();
        if (doc instanceof javax.swing.text.AbstractDocument) {
            ((javax.swing.text.AbstractDocument) doc).readLock();
        }
        try {
            Rectangle alloc = getVisibleEditorRect();
            if (alloc != null) {
                rootView.setSize(alloc.width, alloc.height);
                offs = rootView.viewToModel((float) pt.getX(), (float) pt.getY(), alloc,
                        biasReturn);
            }
        } finally {
            if (doc instanceof javax.swing.text.AbstractDocument) {
                ((javax.swing.text.AbstractDocument) doc).readUnlock();
            }
        }
        return offs;
    }

    public int getNextVisualPositionFrom(JTextComponent t, int pos, Position.Bias b,
            int direction, Position.Bias[] biasRet) throws BadLocationException {
        Document doc = editor.getDocument();
        if (doc instanceof javax.swing.text.AbstractDocument) {
            ((javax.swing.text.AbstractDocument) doc).readLock();
        }
        try {
            Rectangle alloc = getVisibleEditorRect();
            if (alloc != null) {
                rootView.setSize(alloc.width, alloc.height);
            }
            return rootView.getNextVisualPositionFrom(pos, b, alloc, direction, biasRet);
        } finally {
            if (doc instanceof javax.swing.text.AbstractDocument) {
                ((javax.swing.text.AbstractDocument) doc).readUnlock();
            }
        }
    }

    public void damageRange(JTextComponent t, int p0, int p1) {
        damageRange(t, p0, p1, Position.Bias.Forward, Position.Bias.Backward);
    }

    /**
     * It orders the stretch between those two positions to be repainted.
     *
     * <p>If the two fall on the same line it repaints only that piece; if not, it repaints the
     * whole width, because the stretch spills as far as the edge.
     */
    public void damageRange(JTextComponent t, int p0, int p1, Position.Bias p0Bias,
            Position.Bias p1Bias) {
        if (painted) {
            Rectangle alloc = getVisibleEditorRect();
            if (alloc != null) {
                Document doc = t.getDocument();
                if (doc instanceof javax.swing.text.AbstractDocument) {
                    ((javax.swing.text.AbstractDocument) doc).readLock();
                }
                try {
                    rootView.setSize(alloc.width, alloc.height);
                    Shape toDamage = rootView.modelToView(p0, p0Bias, p1, p1Bias, alloc);
                    Rectangle rect = (toDamage instanceof Rectangle)
                            ? (Rectangle) toDamage : toDamage.getBounds();
                    editor.repaint(rect.x, rect.y, rect.width, rect.height);
                } catch (BadLocationException e) {
                    // The stretch stopped existing: there is nothing to repaint.
                } finally {
                    if (doc instanceof javax.swing.text.AbstractDocument) {
                        ((javax.swing.text.AbstractDocument) doc).readUnlock();
                    }
                }
            }
        }
    }

    public EditorKit getEditorKit(JTextComponent tc) {
        return defaultKit;
    }

    private static final EditorKit defaultKit = new DefaultEditorKit();

    public View getRootView(JTextComponent tc) {
        return rootView;
    }

    /** The tool tip text that corresponds to that point; none, unless a view says so. */
    public String getToolTipText(JTextComponent t, Point pt) {
        if (!painted) {
            return null;
        }
        Document doc = editor.getDocument();
        String tt = null;
        Rectangle alloc = getVisibleEditorRect();
        if (alloc != null) {
            if (doc instanceof javax.swing.text.AbstractDocument) {
                ((javax.swing.text.AbstractDocument) doc).readLock();
            }
            try {
                tt = null;
            } finally {
                if (doc instanceof javax.swing.text.AbstractDocument) {
                    ((javax.swing.text.AbstractDocument) doc).readUnlock();
                }
            }
        }
        return tt;
    }

    /** A view for that element; the usual one knows about none in particular. */
    public View create(Element elem) {
        return null;
    }

    /** A view for that stretch of the element. */
    public View create(Element elem, int p0, int p1) {
        return null;
    }

    /**
     * The view that is between the component and the document's view.
     *
     * <p>See the note of the class that contains it: it exists so that the component has a fixed
     * point to ask and in order to translate coordinates.
     */
    static class RootView extends View {

        private final BasicTextUI ui;
        private View view;

        RootView(BasicTextUI ui) {
            super(null);
            this.ui = ui;
        }

        void setView(View v) {
            if (view != null) {
                view.setParent(null);
            }
            view = v;
            if (view != null) {
                view.setParent(this);
            }
        }

        public AttributeSet getAttributes() {
            return null;
        }

        public float getPreferredSpan(int axis) {
            if (view != null) {
                return view.getPreferredSpan(axis);
            }
            return 10;
        }

        public float getMinimumSpan(int axis) {
            if (view != null) {
                return view.getMinimumSpan(axis);
            }
            return 10;
        }

        public float getMaximumSpan(int axis) {
            return Integer.MAX_VALUE;
        }

        public void preferenceChanged(View child, boolean width, boolean height) {
            if (ui.editor != null) {
                ui.editor.revalidate();
            }
        }

        public float getAlignment(int axis) {
            if (view != null) {
                return view.getAlignment(axis);
            }
            return 0;
        }

        public void paint(Graphics g, Shape allocation) {
            if (view != null) {
                Rectangle alloc = (allocation instanceof Rectangle)
                        ? (Rectangle) allocation : allocation.getBounds();
                setSize(alloc.width, alloc.height);
                view.paint(g, allocation);
            }
        }

        public void setParent(View parent) {
            throw new Error("Can't set parent on root view");
        }

        public int getViewCount() {
            return (view != null) ? 1 : 0;
        }

        public View getView(int n) {
            return view;
        }

        public int getViewIndex(int pos, Position.Bias b) {
            return 0;
        }

        public Shape getChildAllocation(int index, Shape a) {
            return a;
        }

        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            if (view != null) {
                return view.modelToView(pos, a, b);
            }
            return null;
        }

        public Shape modelToView(int p0, Position.Bias b0, int p1, Position.Bias b1, Shape a)
                throws BadLocationException {
            if (view != null) {
                return view.modelToView(p0, b0, p1, b1, a);
            }
            return null;
        }

        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            if (view != null) {
                int retValue = view.viewToModel(x, y, a, bias);
                return retValue;
            }
            return -1;
        }

        public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a, int direction,
                Position.Bias[] biasRet) throws BadLocationException {
            if (view != null) {
                int nextPos = view.getNextVisualPositionFrom(pos, b, a, direction, biasRet);
                if (nextPos != -1) {
                    pos = nextPos;
                } else {
                    biasRet[0] = b;
                }
            }
            return pos;
        }

        public void insertUpdate(javax.swing.event.DocumentEvent e, Shape a, ViewFactory f) {
            if (view != null) {
                view.insertUpdate(e, a, f);
            }
        }

        public void removeUpdate(javax.swing.event.DocumentEvent e, Shape a, ViewFactory f) {
            if (view != null) {
                view.removeUpdate(e, a, f);
            }
        }

        public void changedUpdate(javax.swing.event.DocumentEvent e, Shape a, ViewFactory f) {
            if (view != null) {
                view.changedUpdate(e, a, f);
            }
        }

        public Document getDocument() {
            return (ui.editor == null) ? null : ui.editor.getDocument();
        }

        public int getStartOffset() {
            if (view != null) {
                return view.getStartOffset();
            }
            return getElement().getStartOffset();
        }

        public int getEndOffset() {
            if (view != null) {
                return view.getEndOffset();
            }
            return getElement().getEndOffset();
        }

        public Element getElement() {
            if (view != null) {
                return view.getElement();
            }
            return getDocument().getDefaultRootElement();
        }

        public void setSize(float width, float height) {
            if (view != null) {
                view.setSize(width, height);
            }
        }

        public Container getContainer() {
            return ui.editor;
        }

        /** It asks the editor kit first and the look and feel afterwards; see the note. */
        public ViewFactory getViewFactory() {
            EditorKit kit = ui.getEditorKit(ui.editor);
            ViewFactory f = kit.getViewFactory();
            if (f != null) {
                return f;
            }
            return ui;
        }
    }

    /** It listens to the component's changes that force the views to be rebuilt. */
    static class Handler implements PropertyChangeListener {

        private final BasicTextUI ui;

        Handler(BasicTextUI ui) {
            this.ui = ui;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            String name = evt.getPropertyName();
            if ("document".equals(name)) {
                ui.modelChanged();
            }
            ui.propertyChange(evt);
        }
    }

    /** The caret the look and feel sets; it is recognized as its own and can be replaced. */
    public static class BasicCaret extends DefaultCaret implements UIResource {

        public BasicCaret() {
        }
    }

    /** The highlighter the look and feel sets. */
    public static class BasicHighlighter extends DefaultHighlighter implements UIResource {

        public BasicHighlighter() {
        }
    }
}
