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
 * El aspecto comun de todos los componentes de texto.
 *
 * <h2>Que hace, en una linea</h2>
 *
 * <p>Arma un arbol de vistas sobre el documento y lo dibuja. Todo lo demas -- el cursor, el
 * resaltado, las teclas -- lo delega en objetos que se pueden reemplazar.
 *
 * <h2>La vista raiz</h2>
 *
 * <p>Entre el componente y la vista del documento hay una vista de mas, la {@link RootView}. No es
 * decorativa: la vista del documento la cambia el juego de edicion cada vez que cambia el
 * documento, y algo tiene que quedar fijo para que el componente tenga siempre a quien preguntarle.
 * La raiz es ese punto fijo. Ademas es la que traduce entre las coordenadas del componente, que
 * tienen margenes, y las de las vistas, que no.
 *
 * <p>La raiz tambien hace de fabrica: cuando la vista de abajo pide una vista para un elemento, la
 * raiz le pregunta primero al juego de edicion y, si no tiene, al aspecto. Por eso
 * {@code BasicTextUI} implementa {@link ViewFactory}.
 *
 * <h2>Que se dibuja y que no</h2>
 *
 * <p>Sin ventana no hay nada que dibujar, pero las cuentas se hacen igual: el arbol de vistas se
 * arma, se maqueta y contesta {@link #modelToView} y {@link #viewToModel} exactamente igual. Lo
 * unico que falta es el destinatario del {@link Graphics}.
 */
public abstract class BasicTextUI extends TextUI implements ViewFactory {

    private JTextComponent editor;
    private RootView rootView;
    private boolean painted;
    private Handler handler;
    private transient boolean creandoUI;

    /** Un aspecto sin componente; se completa en {@link #installUI}. */
    public BasicTextUI() {
        painted = false;
    }

    /** El cursor que se pone si el componente no trae uno propio. */
    protected Caret createCaret() {
        return new BasicCaret();
    }

    /** El resaltador que se pone si el componente no trae uno propio. */
    protected Highlighter createHighlighter() {
        return new BasicHighlighter();
    }

    /** El nombre del mapa de teclas compartido por los componentes de este tipo. */
    protected String getKeymapName() {
        String nm = getClass().getName();
        int index = nm.lastIndexOf('.');
        if (index >= 0) {
            nm = nm.substring(index + 1, nm.length());
        }
        return nm;
    }

    /**
     * Arma el mapa de teclas.
     *
     * <p>Es compartido entre todos los componentes del mismo tipo: armar uno por componente
     * multiplicaria la misma tabla por cada campo de texto de la pantalla.
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

    /** Reacciona a los cambios del componente que obligan a rehacer las vistas. */
    protected void propertyChange(PropertyChangeEvent evt) {
    }

    /** El prefijo con el que se buscan los valores del aspecto, por ejemplo {@code TextField}. */
    protected abstract String getPropertyPrefix();

    private static final javax.swing.plaf.FontUIResource FUENTE =
            new javax.swing.plaf.FontUIResource("Dialog", java.awt.Font.PLAIN, 12);
    private static final javax.swing.plaf.ColorUIResource FONDO =
            new javax.swing.plaf.ColorUIResource(255, 255, 255);
    private static final javax.swing.plaf.ColorUIResource FRENTE =
            new javax.swing.plaf.ColorUIResource(51, 51, 51);
    private static final javax.swing.plaf.ColorUIResource SELECCION =
            new javax.swing.plaf.ColorUIResource(184, 207, 229);

    /**
     * El margen que le toca a este componente.
     *
     * <p>Cero en los campos y en el area, tres en los dos paneles de edicion. La diferencia tiene
     * sentido: un panel de edicion muestra un documento y el texto pegado al borde se lee mal; un
     * campo de una linea ya viene con el aire que le da su borde. Medido en Metal (JDK 25).
     */
    private java.awt.Insets margenPorOmision() {
        String prefijo = getPropertyPrefix();
        if ("EditorPane".equals(prefijo) || "TextPane".equals(prefijo)) {
            return new javax.swing.plaf.InsetsUIResource(3, 3, 3, 3);
        }
        return new javax.swing.plaf.InsetsUIResource(0, 0, 0, 0);
    }

    /**
     * Pone colores, tipografia y margenes.
     *
     * <p>Solo donde el componente no traiga un valor propio: si sobrescribiera un color puesto a
     * mano, cambiar de aspecto borraria lo que el programa configuro.
     */
    protected void installDefaults() {
        if (editor.getFont() == null || editor.getFont() instanceof UIResource) {
            editor.setFont(FUENTE);
        }
        if (editor.getBackground() == null || editor.getBackground() instanceof UIResource) {
            editor.setBackground(FONDO);
        }
        if (editor.getForeground() == null || editor.getForeground() instanceof UIResource) {
            editor.setForeground(FRENTE);
        }
        if (editor.getCaretColor() == null || editor.getCaretColor() instanceof UIResource) {
            editor.setCaretColor(FRENTE);
        }
        if (editor.getSelectionColor() == null
                || editor.getSelectionColor() instanceof UIResource) {
            editor.setSelectionColor(SELECCION);
        }
        if (editor.getSelectedTextColor() == null
                || editor.getSelectedTextColor() instanceof UIResource) {
            editor.setSelectedTextColor(FRENTE);
        }
        if (editor.getDisabledTextColor() == null
                || editor.getDisabledTextColor() instanceof UIResource) {
            editor.setDisabledTextColor(SELECCION);
        }
        if (editor.getMargin() == null || editor.getMargin() instanceof UIResource) {
            editor.setMargin(margenPorOmision());
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

    /** Rellena el fondo con el color del componente. */
    protected void paintBackground(Graphics g) {
        g.setColor(editor.getBackground());
        g.fillRect(0, 0, editor.getWidth(), editor.getHeight());
    }

    protected final JTextComponent getComponent() {
        return editor;
    }

    /** Rehace el arbol de vistas; se llama cuando el documento cambia. */
    protected void modelChanged() {
        ViewFactory f = rootView.getViewFactory();
        Document doc = editor.getDocument();
        if (doc != null && f != null) {
            Element elem = doc.getDefaultRootElement();
            setView(f.create(elem));
        }
    }

    /** Cuelga esa vista de la raiz. */
    protected final void setView(View v) {
        rootView.setView(v);
        painted = false;
        editor.revalidate();
        editor.repaint();
    }

    /**
     * Dibuja el texto, el resaltado y el cursor, en ese orden.
     *
     * <p>El orden importa: el resaltado va abajo del texto para no taparlo, y el cursor arriba de
     * todo para que se vea sobre la seleccion.
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

        // El documento va primero: instalar el cursor sin documento lo dejaria sin donde pararse.
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
     * Dibuja el componente.
     *
     * <p>Es final: lo que una subclase quiera cambiar va en {@link #paintSafely}, que corre con el
     * documento tomado para lectura. Dibujar sin ese candado podria leer un documento a medio
     * cambiar.
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
                // Todavia sin tamano: se deja crecer para que la vista diga cuanto quiere.
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

    /** El rectangulo donde va el texto: el componente menos los margenes. */
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

    /** Donde cae esa posicion del documento en la pantalla. */
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

    /** Que posicion del documento cae en ese punto de la pantalla. */
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
     * Manda repintar el tramo entre esas dos posiciones.
     *
     * <p>Si las dos caen en la misma linea repinta solo ese pedazo; si no, repinta el ancho
     * entero, porque el tramo se derrama hasta el borde.
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
                    // El tramo dejo de existir: no hay nada que repintar.
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

    /** El texto de ayuda que corresponde a ese punto; ninguno, salvo que una vista lo diga. */
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

    /** Una vista para ese elemento; la de siempre no sabe de ninguno en particular. */
    public View create(Element elem) {
        return null;
    }

    /** Una vista para ese tramo del elemento. */
    public View create(Element elem, int p0, int p1) {
        return null;
    }

    /**
     * La vista que esta entre el componente y la vista del documento.
     *
     * <p>Ver la nota de la clase que la contiene: existe para que el componente tenga un punto fijo
     * al que preguntarle y para traducir coordenadas.
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

        /** Le pregunta primero al juego de edicion y despues al aspecto; ver la nota. */
        public ViewFactory getViewFactory() {
            EditorKit kit = ui.getEditorKit(ui.editor);
            ViewFactory f = kit.getViewFactory();
            if (f != null) {
                return f;
            }
            return ui;
        }
    }

    /** Escucha los cambios del componente que obligan a rehacer las vistas. */
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

    /** El cursor que pone el aspecto; se reconoce como suyo y se puede reemplazar. */
    public static class BasicCaret extends DefaultCaret implements UIResource {

        public BasicCaret() {
        }
    }

    /** El resaltador que pone el aspecto. */
    public static class BasicHighlighter extends DefaultHighlighter implements UIResource {

        public BasicHighlighter() {
        }
    }
}
