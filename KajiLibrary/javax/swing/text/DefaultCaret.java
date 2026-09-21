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
 * The usual cursor: a blinking vertical line, with the selection as a highlight.
 *
 * <h2>It is a rectangle</h2>
 *
 * <p>It inherits from {@link Rectangle}, and that is not an oddity: the cursor <em>is</em> the
 * rectangle it occupies, and having it as fields of its own avoids allocating one on every
 * repaint. The fields {@code x}, {@code y}, {@code width} and {@code height} are its position on
 * the screen.
 *
 * <h2>The cursor does not draw the selection</h2>
 *
 * <p>The cursor puts a highlight in the component's {@link Highlighter} and keeps changing its
 * range. That is why {@link #setSelectionVisible} only adds or removes that highlight: the
 * selection exists in the model even if it is not seen.
 *
 * <h2>The blinking</h2>
 *
 * <p>A {@link Timer} turns it on and off. Without a screen there is nothing to be seen blinking,
 * but the clock runs all the same and the cursor goes from visible to invisible: what is missing
 * is somebody to draw it.
 */
public class DefaultCaret extends Rectangle implements Caret, FocusListener, MouseListener,
        MouseMotionListener {

    /** Update the position only if the change came from the event thread. */
    public static final int UPDATE_WHEN_ON_EDT = 0;

    /** Never fix up the position when the document changes. */
    public static final int NEVER_UPDATE = 1;

    /** Always fix it up. */
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

    /** A cursor at position zero, invisible until it is installed. */
    public DefaultCaret() {
        dotBias = Position.Bias.Forward;
        markBias = Position.Bias.Forward;
    }

    /** When to fix up the position on a change of the document. */
    public void setUpdatePolicy(int policy) {
        updatePolicy = policy;
    }

    public int getUpdatePolicy() {
        return updatePolicy;
    }

    protected final JTextComponent getComponent() {
        return component;
    }

    /** It asks for a repaint where the cursor is. */
    protected final synchronized void repaint() {
        if (component != null) {
            component.repaint(x, y, width, height);
        }
    }

    /**
     * It computes the cursor's rectangle and asks for a repaint.
     *
     * <p>It repaints the old and the new: if it repainted only the new, a cursor would be left
     * drawn where it no longer is.
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

    /** It asks the component to scroll so that the cursor is seen. */
    protected void adjustVisibility(Rectangle nloc) {
        if (component == null) {
            return;
        }
        component.scrollRectToVisible(nloc);
    }

    /** Who paints the selection: the highlighter's default painter. */
    protected Highlighter.HighlightPainter getSelectionPainter() {
        return DefaultHighlighter.DefaultPainter;
    }

    /** It takes the cursor to where the click happened. */
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

    /** It extends the selection to where the drag went. */
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

    /** A click places the cursor; with Shift, it extends the selection. */
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

    /** Dragging extends the selection. */
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            moveCaret(e);
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

    /** It draws the line, if it is visible. */
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
                    // It moved without anybody repainting: the rectangle is fixed up.
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
                // It could not be placed: nothing is drawn.
            }
        }
    }

    /** It hooks itself to the component and to its document. */
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

    /** It reports that the cursor moved; the component forwards it as a caret event. */
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

    /** It adds or removes the selection's highlight; see the class note. */
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

    /** Whether the cursor is on at this instant of the blinking. */
    public boolean isActive() {
        return active;
    }

    public boolean isVisible() {
        return visible;
    }

    /** It turns the cursor on or off, and with it, the blinking clock. */
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

    /** How often it blinks; zero leaves it steady. */
    public void setBlinkRate(int rate) {
        if (rate != 0) {
            if (flasher == null) {
                flasher = new Timer(rate, new Blink(this));
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

    /** It moves the cursor and undoes the selection. */
    public void setDot(int dot) {
        setDot(dot, Position.Bias.Forward);
    }

    /** It moves the cursor and extends the selection. */
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
                        // The stretch stopped existing: it is ignored.
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

    /** It changes the position, repaints the old and the new, and reports. */
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

    /** It recomputes the rectangle if the component changed size. */
    private void validateBounds() {
    }

    /** The column the cursor remembers when going up and down; see {@link Caret}. */
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

    /** The line's width; one, unless the component asks for another. */
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

    /** The shortcut the navigation filter uses to really move the cursor. */
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

    /** It turns the cursor on and off; the clock calls it. */
    static class Blink implements ActionListener {

        private final DefaultCaret cursor;

        Blink(DefaultCaret cursor) {
            this.cursor = cursor;
        }

        public void actionPerformed(ActionEvent e) {
            cursor.active = !cursor.active;
            cursor.repaint();
        }
    }

    /**
     * It fixes up the position when the document changes.
     *
     * <p>If something was inserted before the cursor, the cursor shifts; if something was removed
     * around it, it sticks to the gap. Without this, typing at the start of a text would leave the
     * cursor where it was and the user would see their cursor "go backwards".
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
