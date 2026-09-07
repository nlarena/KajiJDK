package javax.swing.text;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.EventListener;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;

/**
 * El cursor de siempre: una raya vertical que parpadea, con la seleccion como resaltado.
 *
 * <h2>Es un rectangulo</h2>
 *
 * <p>Hereda de {@link Rectangle}, y eso no es una rareza: el cursor <em>es</em> el rectangulo que
 * ocupa, y tenerlo como campos propios evita reservar uno por cada repintado. Los campos {@code x},
 * {@code y}, {@code width} y {@code height} son su posicion en la pantalla.
 *
 * <h2>La seleccion no la dibuja el cursor</h2>
 *
 * <p>El cursor pone un resaltado en el {@link Highlighter} del componente y lo va cambiando de
 * rango. Por eso {@link #setSelectionVisible} solo agrega o saca ese resaltado: la seleccion
 * existe en el modelo aunque no se vea.
 *
 * <h2>El parpadeo</h2>
 *
 * <p>Un {@link Timer} lo prende y lo apaga. Sin pantalla no hay nada que se vea parpadear, pero el
 * reloj corre igual y el cursor cambia de visible a invisible: lo que falta es quien lo dibuje.
 */
public class DefaultCaret extends Rectangle implements Caret, FocusListener, MouseListener,
        MouseMotionListener {

    /** Actualizar la posicion solo si el cambio vino del hilo de eventos. */
    public static final int UPDATE_WHEN_ON_EDT = 0;

    /** No acomodar nunca la posicion cuando el documento cambia. */
    public static final int NEVER_UPDATE = 1;

    /** Acomodarla siempre. */
    public static final int ALWAYS_UPDATE = 2;

    protected EventListenerList listenerList = new EventListenerList();

    protected transient ChangeEvent changeEvent = null;

    JTextComponent component;
    int updatePolicy = UPDATE_WHEN_ON_EDT;
    boolean visible;
    boolean active;
    int dot;
    int mark;
    Object selectionTag;
    boolean selectionVisible;
    Timer flasher;
    Point magicCaretPosition;
    transient Position.Bias dotBias;
    transient Position.Bias markBias;
    boolean dotLTR = true;
    boolean markLTR = true;
    transient Handler handler = new Handler(this);

    private transient int[] flagXPoints = new int[3];
    private transient int[] flagYPoints = new int[3];

    /** Un cursor en la posicion cero, invisible hasta que lo instalen. */
    public DefaultCaret() {
        dotBias = Position.Bias.Forward;
        markBias = Position.Bias.Forward;
    }

    /** Cuando acomodar la posicion ante un cambio del documento. */
    public void setUpdatePolicy(int policy) {
        updatePolicy = policy;
    }

    public int getUpdatePolicy() {
        return updatePolicy;
    }

    protected final JTextComponent getComponent() {
        return component;
    }

    /** Manda repintar donde esta el cursor. */
    protected final synchronized void repaint() {
        if (component != null) {
            component.repaint(x, y, width, height);
        }
    }

    /**
     * Calcula el rectangulo del cursor y lo manda repintar.
     *
     * <p>Repinta lo viejo y lo nuevo: si solo repintara lo nuevo, quedaria un cursor dibujado
     * donde ya no esta.
     */
    protected synchronized void damage(Rectangle r) {
        if (r != null) {
            int damageWidth = getCaretWidth(r.height);
            x = r.x - 4 - (damageWidth >> 1);
            y = r.y;
            width = 9 + damageWidth;
            height = r.height;
            repaint();
        }
    }

    /** Pide que el componente se desplace para que el cursor se vea. */
    protected void adjustVisibility(Rectangle nloc) {
        if (component == null) {
            return;
        }
        component.scrollRectToVisible(nloc);
    }

    /** Quien pinta la seleccion: el pintor por omision del resaltador. */
    protected Highlighter.HighlightPainter getSelectionPainter() {
        return DefaultHighlighter.DefaultPainter;
    }

    /** Lleva el cursor a donde se hizo clic. */
    protected void positionCaret(MouseEvent e) {
        Point pt = new Point(e.getX(), e.getY());
        Position.Bias[] biasRet = new Position.Bias[1];
        int pos = component.getUI().viewToModel(component, pt, biasRet);
        if (biasRet[0] == null) {
            biasRet[0] = Position.Bias.Forward;
        }
        if (pos >= 0) {
            setDot(pos, biasRet[0]);
        }
    }

    /** Extiende la seleccion hasta donde se arrastro. */
    protected void moveCaret(MouseEvent e) {
        Point pt = new Point(e.getX(), e.getY());
        Position.Bias[] biasRet = new Position.Bias[1];
        int pos = component.getUI().viewToModel(component, pt, biasRet);
        if (biasRet[0] == null) {
            biasRet[0] = Position.Bias.Forward;
        }
        if (pos >= 0) {
            moveDot(pos, biasRet[0]);
        }
    }

    public void focusGained(FocusEvent e) {
        if (component.isEnabled()) {
            if (component.isEditable()) {
                setVisible(true);
            }
            setSelectionVisible(true);
        }
    }

    public void focusLost(FocusEvent e) {
        setVisible(false);
        setSelectionVisible(false);
    }

    public void mouseClicked(MouseEvent e) {
    }

    /** Un clic ubica el cursor; con Shift, extiende la seleccion. */
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            adjustCaretAndFocus(e);
        }
    }

    void adjustCaretAndFocus(MouseEvent e) {
        if (component != null && component.isEnabled()) {
            positionCaret(e);
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    /** Arrastrar extiende la seleccion. */
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            moveCaret(e);
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

    /** Dibuja la raya, si esta visible. */
    public void paint(Graphics g) {
        if (isVisible()) {
            try {
                JTextComponent comp = component;
                if (comp == null) {
                    return;
                }
                Rectangle r = comp.getUI().modelToView(comp, dot, dotBias);
                if (r == null) {
                    return;
                }
                if ((r.width == 0) && (r.height == 0)) {
                    return;
                }
                if (width > 0 && height > 0 && !this.contains(r.x, r.y, r.width, r.height)) {
                    // Se movio sin que nadie repintara: se acomoda el rectangulo.
                    Rectangle clip = g.getClipBounds();
                    if (clip != null && !clip.contains(this)) {
                        repaint();
                    }
                    damage(r);
                }
                g.setColor(comp.getCaretColor());
                int paintWidth = getCaretWidth(r.height);
                r.x = r.x - paintWidth / 2;
                g.fillRect(r.x, r.y, paintWidth, r.height);
            } catch (BadLocationException e) {
                // No se pudo ubicar: no se dibuja nada.
            }
        }
    }

    /** Se engancha al componente y a su documento. */
    public void install(JTextComponent c) {
        component = c;
        Document doc = c.getDocument();
        dot = 0;
        mark = 0;
        if (doc != null) {
            doc.addDocumentListener(handler);
        }
        c.addPropertyChangeListener(handler);
        c.addFocusListener(this);
        c.addMouseListener(this);
        c.addMouseMotionListener(this);
        if (component.hasFocus()) {
            focusGained(null);
        }
    }

    public void deinstall(JTextComponent c) {
        c.removeMouseListener(this);
        c.removeMouseMotionListener(this);
        c.removeFocusListener(this);
        c.removePropertyChangeListener(handler);
        Document doc = c.getDocument();
        if (doc != null) {
            doc.removeDocumentListener(handler);
        }
        synchronized (this) {
            component = null;
        }
        if (flasher != null) {
            flasher.stop();
        }
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /** Avisa que el cursor se movio; el componente lo reenvia como evento de cursor. */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /** Pone o saca el resaltado de la seleccion; ver la nota de la clase. */
    public void setSelectionVisible(boolean vis) {
        if (vis != selectionVisible) {
            selectionVisible = vis;
            if (selectionVisible) {
                Highlighter h = component.getHighlighter();
                if ((dot != mark) && (h != null) && (selectionTag == null)) {
                    int p0 = Math.min(dot, mark);
                    int p1 = Math.max(dot, mark);
                    Highlighter.HighlightPainter p = getSelectionPainter();
                    try {
                        selectionTag = h.addHighlight(p0, p1, p);
                    } catch (BadLocationException bl) {
                        selectionTag = null;
                    }
                }
            } else {
                if (selectionTag != null) {
                    Highlighter h = component.getHighlighter();
                    h.removeHighlight(selectionTag);
                    selectionTag = null;
                }
            }
        }
    }

    public boolean isSelectionVisible() {
        return selectionVisible;
    }

    /** Si el cursor esta prendido en este instante del parpadeo. */
    public boolean isActive() {
        return active;
    }

    public boolean isVisible() {
        return visible;
    }

    /** Prende o apaga el cursor, y con el, el reloj del parpadeo. */
    public void setVisible(boolean e) {
        if (component != null) {
            validateBounds();
        }
        if (this.visible != e) {
            this.visible = e;
            if (flasher != null) {
                if (visible) {
                    flasher.start();
                } else {
                    flasher.stop();
                }
            }
            repaint();
        }
        active = e;
    }

    /** Cada cuanto parpadea; cero lo deja fijo. */
    public void setBlinkRate(int rate) {
        if (rate != 0) {
            if (flasher == null) {
                flasher = new Timer(rate, new Parpadeo(this));
            }
            flasher.setDelay(rate);
        } else {
            if (flasher != null) {
                flasher.stop();
                flasher = null;
            }
        }
    }

    public int getBlinkRate() {
        return (flasher == null) ? 0 : flasher.getDelay();
    }

    public int getDot() {
        return dot;
    }

    public int getMark() {
        return mark;
    }

    /** Mueve el cursor y deshace la seleccion. */
    public void setDot(int dot) {
        setDot(dot, Position.Bias.Forward);
    }

    /** Mueve el cursor y extiende la seleccion. */
    public void moveDot(int dot) {
        moveDot(dot, Position.Bias.Forward);
    }

    public void moveDot(int dot, Position.Bias dotBias) {
        if (dotBias == null) {
            throw new IllegalArgumentException("null bias");
        }
        if (!component.isEnabled()) {
            setDot(dot, dotBias);
            return;
        }
        NavigationFilter filter = component.getNavigationFilter();
        if (filter != null) {
            filter.moveDot(getFilterBypass(), dot, dotBias);
        } else {
            handleMoveDot(dot, dotBias);
        }
    }

    void handleMoveDot(int dot, Position.Bias dotBias) {
        changeCaretPosition(dot, dotBias);
        if (selectionVisible) {
            Highlighter h = component.getHighlighter();
            if (h != null) {
                int p0 = Math.min(dot, mark);
                int p1 = Math.max(dot, mark);
                if (p0 == p1) {
                    if (selectionTag != null) {
                        h.removeHighlight(selectionTag);
                        selectionTag = null;
                    }
                } else {
                    try {
                        if (selectionTag != null) {
                            h.changeHighlight(selectionTag, p0, p1);
                        } else {
                            Highlighter.HighlightPainter p = getSelectionPainter();
                            selectionTag = h.addHighlight(p0, p1, p);
                        }
                    } catch (BadLocationException e) {
                        // El tramo dejo de existir: se ignora.
                    }
                }
            }
        }
    }

    public void setDot(int dot, Position.Bias dotBias) {
        if (dotBias == null) {
            throw new IllegalArgumentException("null bias");
        }
        NavigationFilter filter = component.getNavigationFilter();
        if (filter != null) {
            filter.setDot(getFilterBypass(), dot, dotBias);
        } else {
            handleSetDot(dot, dotBias);
        }
    }

    void handleSetDot(int dot, Position.Bias dotBias) {
        Document doc = component.getDocument();
        if (doc != null) {
            dot = Math.min(dot, doc.getLength());
        }
        dot = Math.max(dot, 0);
        if (dot == 0) {
            dotBias = Position.Bias.Forward;
        }
        mark = dot;
        if (selectionTag != null) {
            Highlighter h = component.getHighlighter();
            if (h != null) {
                h.removeHighlight(selectionTag);
            }
            selectionTag = null;
        }
        changeCaretPosition(dot, dotBias);
        markBias = this.dotBias;
        markLTR = dotLTR;
    }

    public Position.Bias getDotBias() {
        return dotBias;
    }

    public Position.Bias getMarkBias() {
        return markBias;
    }

    boolean isDotLeftToRight() {
        return dotLTR;
    }

    boolean isMarkLeftToRight() {
        return markLTR;
    }

    boolean isPositionLTR(int position, Position.Bias bias) {
        return true;
    }

    Position.Bias guessBiasForOffset(int offset, Position.Bias lastBias, boolean lastLTR) {
        return Position.Bias.Forward;
    }

    /** Cambia la posicion, repinta lo viejo y lo nuevo, y avisa. */
    void changeCaretPosition(int dot, Position.Bias dotBias) {
        repaint();
        this.dot = dot;
        this.dotBias = dotBias;
        setMagicCaretPosition(null);
        fireStateChanged();
        repaintNewCaret();
    }

    void repaintNewCaret() {
        if (component != null) {
            javax.swing.plaf.TextUI mapper = component.getUI();
            Document doc = component.getDocument();
            if ((mapper != null) && (doc != null)) {
                Rectangle newLoc;
                try {
                    newLoc = mapper.modelToView(component, this.dot, this.dotBias);
                } catch (BadLocationException e) {
                    newLoc = null;
                }
                if (newLoc != null) {
                    adjustVisibility(newLoc);
                    if (getMagicCaretPosition() == null) {
                        setMagicCaretPosition(new Point(newLoc.x, newLoc.y));
                    }
                }
                damage(newLoc);
            }
        }
    }

    /** Recalcula el rectangulo si el componente cambio de tamano. */
    private void validateBounds() {
    }

    /** La columna que el cursor recuerda al subir y bajar; ver {@link Caret}. */
    public void setMagicCaretPosition(Point p) {
        magicCaretPosition = p;
    }

    public Point getMagicCaretPosition() {
        return magicCaretPosition;
    }

    public boolean equals(Object obj) {
        return (this == obj);
    }

    public String toString() {
        String s = "Dot=(" + dot + ", " + dotBias + ")";
        s = s + " Mark=(" + mark + ", " + markBias + ")";
        return s;
    }

    /** El ancho de la raya; uno, salvo que el componente pida otro. */
    int getCaretWidth(int height) {
        return 1;
    }

    private NavigationFilter.FilterBypass filterBypass;

    private NavigationFilter.FilterBypass getFilterBypass() {
        if (filterBypass == null) {
            filterBypass = new DefaultFilterBypass(this);
        }
        return filterBypass;
    }

    /** El atajo que usa el filtro de navegacion para mover el cursor de verdad. */
    static class DefaultFilterBypass extends NavigationFilter.FilterBypass {

        private final DefaultCaret cursor;

        DefaultFilterBypass(DefaultCaret cursor) {
            this.cursor = cursor;
        }

        public Caret getCaret() {
            return cursor;
        }

        public void setDot(int dot, Position.Bias bias) {
            cursor.handleSetDot(dot, bias);
        }

        public void moveDot(int dot, Position.Bias bias) {
            cursor.handleMoveDot(dot, bias);
        }
    }

    /** Prende y apaga el cursor; lo llama el reloj. */
    static class Parpadeo implements ActionListener {

        private final DefaultCaret cursor;

        Parpadeo(DefaultCaret cursor) {
            this.cursor = cursor;
        }

        public void actionPerformed(ActionEvent e) {
            cursor.active = !cursor.active;
            cursor.repaint();
        }
    }

    /**
     * Acomoda la posicion cuando el documento cambia.
     *
     * <p>Si se inserto antes del cursor, el cursor se corre; si se borro alrededor, se pega al
     * hueco. Sin esto, escribir al principio de un texto dejaria el cursor donde estaba y el
     * usuario veria su cursor "retroceder".
     */
    static class Handler implements DocumentListener, java.beans.PropertyChangeListener {

        private final DefaultCaret cursor;

        Handler(DefaultCaret cursor) {
            this.cursor = cursor;
        }

        public void insertUpdate(DocumentEvent e) {
            int offset = e.getOffset();
            int length = e.getLength();
            int newDot = cursor.dot;
            if (newDot >= offset) {
                newDot = newDot + length;
            }
            int newMark = cursor.mark;
            if (newMark >= offset) {
                newMark = newMark + length;
            }
            if (newMark == newDot) {
                cursor.setDot(newDot);
            } else {
                cursor.setDot(newMark);
                if (cursor.getDot() == newMark) {
                    cursor.moveDot(newDot);
                }
            }
        }

        public void removeUpdate(DocumentEvent e) {
            int offs0 = e.getOffset();
            int offs1 = offs0 + e.getLength();
            int newDot = cursor.dot;
            if (newDot >= offs1) {
                newDot = newDot - (offs1 - offs0);
            } else if (newDot >= offs0) {
                newDot = offs0;
            }
            int newMark = cursor.mark;
            if (newMark >= offs1) {
                newMark = newMark - (offs1 - offs0);
            } else if (newMark >= offs0) {
                newMark = offs0;
            }
            if (newMark == newDot) {
                cursor.setDot(newDot);
            } else {
                cursor.setDot(newMark);
                if (cursor.getDot() == newMark) {
                    cursor.moveDot(newDot);
                }
            }
        }

        public void changedUpdate(DocumentEvent e) {
        }

        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            Object oldValue = evt.getOldValue();
            Object newValue = evt.getNewValue();
            if ((oldValue instanceof Document) || (newValue instanceof Document)) {
                if (oldValue != null) {
                    ((Document) oldValue).removeDocumentListener(this);
                }
                if (newValue != null) {
                    ((Document) newValue).addDocumentListener(this);
                }
                cursor.setDot(0);
            }
        }
    }
}
