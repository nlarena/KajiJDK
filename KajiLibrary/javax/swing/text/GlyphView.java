package javax.swing.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;

import javax.swing.event.DocumentEvent;

/**
 * A stretch of text with a single font and a single colour: the view that really draws
 * letters.
 *
 * <h2>The painter</h2>
 *
 * <p>The view does not draw: it asks a {@link GlyphPainter}. The separation looks unnecessary
 * and is not: the same stretch is drawn differently according to the platform --with or without
 * complex shapes, left to right or the reverse-- and changing painter is changing all that
 * without touching the view. Here there is a single painter, the one that measures with
 * {@link FontMetrics} and draws with {@code drawChars}.
 *
 * <h2>Fragments</h2>
 *
 * <p>When a stretch does not fit on a line, the view <em>splits</em>: {@link #breakView}
 * returns a fragment that shows only a piece of the same element. The fragment shares everything
 * with the original except two numbers, {@code offset} and {@code length}, and that is why
 * splitting is cheap. That is the reason the class is {@code Cloneable}.
 */
public class GlyphView extends View implements TabableView, Cloneable {

    int offset;
    int length;

    /** Whether the stretch ends with an implicit line ending. */
    boolean impliedCR;

    /** Whether this stretch's width does not count when measuring the line. */
    boolean skipWidth;

    TabExpander expander;

    /** Where it starts, for expanding tabs. */
    int x;

    GlyphPainter painter;

    static GlyphPainter defaultPainter;

    /** A view of that element, whole. */
    public GlyphView(Element elem) {
        super(elem);
        offset = 0;
        length = 0;
    }

    /** A copy; {@link #createFragment} uses it. */
    protected final Object clone() {
        Object o;
        try {
            o = super.clone();
        } catch (CloneNotSupportedException cnse) {
            o = null;
        }
        return o;
    }

    public GlyphPainter getGlyphPainter() {
        return painter;
    }

    public void setGlyphPainter(GlyphPainter p) {
        painter = p;
    }

    /** The stretch's text, without copying when it can. */
    public Segment getText(int p0, int p1) {
        Segment text = new Segment();
        try {
            Document doc = getDocument();
            doc.getText(p0, p1 - p0, text);
        } catch (BadLocationException bl) {
            throw new StateInvariantError("GlyphView: Stale view: " + bl);
        }
        return text;
    }

    /** The stretch's background colour, or {@code null} if it is transparent. */
    public Color getBackground() {
        Document doc = getDocument();
        if (doc instanceof StyledDocument) {
            AttributeSet attr = getAttributes();
            if (attr.isDefined(StyleConstants.Background)) {
                return ((StyledDocument) doc).getBackground(attr);
            }
        }
        return null;
    }

    public Color getForeground() {
        Document doc = getDocument();
        if (doc instanceof StyledDocument) {
            AttributeSet attr = getAttributes();
            return ((StyledDocument) doc).getForeground(attr);
        }
        java.awt.Container c = getContainer();
        if (c != null) {
            return c.getForeground();
        }
        return null;
    }

    public Font getFont() {
        Document doc = getDocument();
        if (doc instanceof StyledDocument) {
            AttributeSet attr = getAttributes();
            return ((StyledDocument) doc).getFont(attr);
        }
        java.awt.Container c = getContainer();
        if (c != null) {
            return c.getFont();
        }
        return null;
    }

    public boolean isUnderline() {
        AttributeSet attr = getAttributes();
        return StyleConstants.isUnderline(attr);
    }

    public boolean isStrikeThrough() {
        AttributeSet attr = getAttributes();
        return StyleConstants.isStrikeThrough(attr);
    }

    public boolean isSubscript() {
        AttributeSet attr = getAttributes();
        return StyleConstants.isSubscript(attr);
    }

    public boolean isSuperscript() {
        AttributeSet attr = getAttributes();
        return StyleConstants.isSuperscript(attr);
    }

    /** Who knows where the tabs fall; the parent, if it knows. */
    public TabExpander getTabExpander() {
        return expander;
    }

    /** It makes sure there is a painter; it creates it the first time. */
    protected void checkPainter() {
        if (painter == null) {
            if (defaultPainter == null) {
                defaultPainter = new SimplePainter();
            }
            setGlyphPainter(defaultPainter.getPainter(this, getStartOffset(), getEndOffset()));
        }
    }

    public float getTabbedSpan(float x, TabExpander e) {
        checkPainter();
        TabExpander old = expander;
        expander = e;
        if (expander != old) {
            preferenceChanged(null, true, false);
        }
        this.x = (int) x;
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        float width = painter.getSpan(this, p0, p1, expander, x);
        return width;
    }

    public float getPartialSpan(int p0, int p1) {
        checkPainter();
        float width = painter.getSpan(this, p0, p1, expander, 0f);
        return width;
    }

    public int getStartOffset() {
        Element e = getElement();
        return (length > 0) ? e.getStartOffset() + offset : e.getStartOffset();
    }

    public int getEndOffset() {
        Element e = getElement();
        return (length > 0) ? e.getStartOffset() + offset + length : e.getEndOffset();
    }

    /** It paints the background if there is one, the text, and the underline or strike lines. */
    public void paint(Graphics g, Shape a) {
        checkPainter();

        boolean paintedText = false;
        java.awt.Container c = getContainer();
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
        Color bg = getBackground();
        Color fg = getForeground();

        if (bg != null) {
            g.setColor(bg);
            g.fillRect(alloc.x, alloc.y, alloc.width, alloc.height);
        }

        if (c instanceof JTextComponent) {
            JTextComponent tc = (JTextComponent) c;
            Highlighter h = tc.getHighlighter();
            if (h instanceof LayeredHighlighter) {
                ((LayeredHighlighter) h).paintLayeredHighlights(g, p0, p1, a, tc, this);
            }
        }

        if (!paintedText) {
            g.setColor(fg);
            painter.paint(this, g, a, p0, p1);
        }

        if (isUnderline() || isStrikeThrough()) {
            FontMetrics fm = getMetrics();
            int y = alloc.y + (int) painter.getAscent(this);
            int x0 = alloc.x;
            int x1 = alloc.x + alloc.width;
            if (isUnderline()) {
                int yTmp = y + 1;
                g.drawLine(x0, yTmp, x1, yTmp);
            }
            if (isStrikeThrough()) {
                int yTmp = y - (int) (painter.getAscent(this) * 0.3f);
                g.drawLine(x0, yTmp, x1, yTmp);
            }
        }
    }

    /** It paints the stretch in that colour; whoever paints the selection uses it. */
    final void paintTextUsingColor(Graphics g, Shape a, Color c, int p0, int p1) {
        g.setColor(c);
        painter.paint(this, g, a, p0, p1);
    }

    public float getMinimumSpan(int axis) {
        if (axis == View.X_AXIS) {
            checkPainter();
            // The minimum is the longest word: less than that cannot be broken.
            return getPartialSpan(getStartOffset(), getEndOffset());
        }
        return getPreferredSpan(axis);
    }

    public float getPreferredSpan(int axis) {
        if (skipWidth && axis == X_AXIS) {
            return 0;
        }
        checkPainter();
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        // if/else and not switch: View's constants are read from a `.class` and are not folded
        // (#503).
        if (axis == View.X_AXIS) {
            if (impliedCR) {
                return 0;
            }
            return painter.getSpan(this, p0, p1, expander, this.x);
        }
        if (axis == View.Y_AXIS) {
            float h = painter.getHeight(this);
            if (isSuperscript()) {
                h = h + h / 3;
            }
            return h;
        }
        throw new IllegalArgumentException("Invalid axis: " + axis);
    }

    /** It is aligned by its baseline, not by its box: it is what makes the text readable. */
    public float getAlignment(int axis) {
        checkPainter();
        if (axis == View.Y_AXIS) {
            boolean sup = isSuperscript();
            boolean sub = isSubscript();
            float h = painter.getHeight(this);
            float d = painter.getDescent(this);
            float a = painter.getAscent(this);
            float align;
            if (sup) {
                align = 1.0f;
            } else if (sub) {
                align = (h > 0) ? (h - (d + (a / 2))) / h : 0;
            } else {
                align = (h > 0) ? (h - d) / h : 0;
            }
            return align;
        }
        return super.getAlignment(axis);
    }

    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        checkPainter();
        return painter.modelToView(this, pos, b, a);
    }

    public int viewToModel(float x, float y, Shape a, Position.Bias[] biasReturn) {
        checkPainter();
        return painter.viewToModel(this, x, y, a, biasReturn);
    }

    /**
     * How well it breaks at that point.
     *
     * <p>Excellent if there is a space to break at, bad if a word would have to be split. It is
     * what makes a paragraph break at spaces.
     */
    public int getBreakWeight(int axis, float pos, float len) {
        if (axis == View.X_AXIS) {
            checkPainter();
            int p0 = getStartOffset();
            int p1 = painter.getBoundedPosition(this, p0, pos, len);
            if (p1 == p0) {
                return View.BadBreakWeight;
            }
            if (getBreakSpot(p0, p1) != -1) {
                return View.ExcellentBreakWeight;
            }
            return View.GoodBreakWeight;
        }
        return super.getBreakWeight(axis, pos, len);
    }

    /** Where there is a space to break at, looking from back to front. */
    private int getBreakSpot(int p0, int p1) {
        Segment s = getText(p0, p1);
        for (int i = s.offset + s.count - 1; i >= s.offset; i--) {
            char ch = s.array[i];
            if (Character.isWhitespace(ch)) {
                return p0 + (i - s.offset) + 1;
            }
        }
        return -1;
    }

    /** It splits to fit in that room; it returns the piece that fits. */
    public View breakView(int axis, int p0, float pos, float len) {
        if (axis == View.X_AXIS) {
            checkPainter();
            int p1 = painter.getBoundedPosition(this, p0, pos, len);
            int breakSpot = getBreakSpot(p0, p1);
            if (breakSpot != -1) {
                p1 = breakSpot;
            }
            if (p1 == p0) {
                return this;
            }
            return createFragment(p0, p1);
        }
        return this;
    }

    /** A fragment that shows that piece; see the class note. */
    public View createFragment(int p0, int p1) {
        checkPainter();
        Element elem = getElement();
        GlyphView v = (GlyphView) clone();
        v.offset = p0 - elem.getStartOffset();
        v.length = p1 - p0;
        v.painter = painter.getPainter(v, p0, p1);
        return v;
    }

    public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a, int direction,
            Position.Bias[] biasRet) throws BadLocationException {
        return painter != null
                ? painter.getNextVisualPositionFrom(this, pos, b, a, direction, biasRet)
                : super.getNextVisualPositionFrom(pos, b, a, direction, biasRet);
    }

    public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        preferenceChanged(null, true, false);
    }

    public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        preferenceChanged(null, true, false);
    }

    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        // The attributes changed: the font may have changed, it has to be measured again.
        painter = null;
        preferenceChanged(null, true, true);
    }

    void updateAfterChange() {
    }

    /** Data for justifying the text; without justification, none. */
    JustificationInfo getJustificationInfo(int rowStartOffset) {
        return null;
    }

    /** The metrics of this stretch's font. */
    private FontMetrics getMetrics() {
        Font f = getFont();
        java.awt.Container c = getContainer();
        if (c != null && f != null) {
            return c.getFontMetrics(f);
        }
        if (f != null) {
            return Toolkit.getDefaultToolkit().getFontMetrics(f);
        }
        return null;
    }

    /**
     * Who draws and measures a {@link GlyphView}'s letters.
     *
     * <p>With no state of its own: it receives the view on every call. That is why one same painter
     * can serve thousands of stretches, and why {@link #getPainter} can return itself.
     */
    public abstract static class GlyphPainter {

        protected GlyphPainter() {
        }

        /** How much that stretch takes up starting at {@code x}. */
        public abstract float getSpan(GlyphView v, int p0, int p1, TabExpander e, float x);

        public abstract float getHeight(GlyphView v);

        public abstract float getAscent(GlyphView v);

        public abstract float getDescent(GlyphView v);

        public abstract void paint(GlyphView v, Graphics g, Shape a, int p0, int p1);

        public abstract Shape modelToView(GlyphView v, int pos, Position.Bias bias, Shape a)
                throws BadLocationException;

        public abstract int viewToModel(GlyphView v, float x, float y, Shape a,
                Position.Bias[] biasReturn);

        /** How far the text that fits in {@code len} pixels reaches. */
        public abstract int getBoundedPosition(GlyphView v, int p0, float x, float len);

        /** The painter that corresponds to that fragment; by default, this same one. */
        public GlyphPainter getPainter(GlyphView v, int p0, int p1) {
            return this;
        }

        public int getNextVisualPositionFrom(GlyphView v, int pos, Position.Bias b, Shape a,
                int direction, Position.Bias[] biasRet) throws BadLocationException {
            int startOffset = v.getStartOffset();
            int endOffset = v.getEndOffset();
            biasRet[0] = Position.Bias.Forward;
            if (direction == View.EAST) {
                if (pos == -1) {
                    return startOffset;
                }
                if (pos + 1 >= endOffset) {
                    return -1;
                }
                return pos + 1;
            }
            if (direction == View.WEST) {
                if (pos == -1) {
                    return endOffset - 1;
                }
                if (pos - 1 < startOffset) {
                    return -1;
                }
                return pos - 1;
            }
            return pos;
        }
    }

    /** Justification data; without justification, it is not used. */
    static class JustificationInfo {

        final int start;
        final int end;
        final int leadingSpaces;
        final int contentSpaces;
        final int trailingSpaces;
        final boolean hasTab;

        JustificationInfo(int start, int end, int leadingSpaces, int contentSpaces,
                int trailingSpaces, boolean hasTab) {
            this.start = start;
            this.end = end;
            this.leadingSpaces = leadingSpaces;
            this.contentSpaces = contentSpaces;
            this.trailingSpaces = trailingSpaces;
            this.hasTab = hasTab;
        }
    }

    /**
     * This library's painter: it measures with {@link FontMetrics} and draws with
     * {@code drawChars}.
     *
     * <p>It is the equivalent of the JDK's {@code GlyphPainter1}, which is the one used when the
     * text does not need complex shapes. This VM's rasterizer does not have them, so this one is
     * enough for everything.
     */
    static class SimplePainter extends GlyphPainter {

        private FontMetrics metrics(GlyphView v) {
            Font f = v.getFont();
            java.awt.Container c = v.getContainer();
            if (c != null && f != null) {
                return c.getFontMetrics(f);
            }
            if (f != null) {
                return Toolkit.getDefaultToolkit().getFontMetrics(f);
            }
            return null;
        }

        public float getSpan(GlyphView v, int p0, int p1, TabExpander e, float x) {
            FontMetrics fm = metrics(v);
            if (fm == null) {
                return 0;
            }
            Segment text = v.getText(p0, p1);
            return Utilities.getTabbedTextWidth(text, fm, x, e, p0);
        }

        public float getHeight(GlyphView v) {
            FontMetrics fm = metrics(v);
            return (fm != null) ? fm.getHeight() : 0;
        }

        public float getAscent(GlyphView v) {
            FontMetrics fm = metrics(v);
            return (fm != null) ? fm.getAscent() : 0;
        }

        public float getDescent(GlyphView v) {
            FontMetrics fm = metrics(v);
            return (fm != null) ? fm.getDescent() : 0;
        }

        public void paint(GlyphView v, Graphics g, Shape a, int p0, int p1) {
            FontMetrics fm = metrics(v);
            if (fm == null) {
                return;
            }
            Segment text = v.getText(p0, p1);
            Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
            g.setFont(v.getFont());
            Utilities.drawTabbedText(text, alloc.x, alloc.y + fm.getAscent(), g,
                    v.getTabExpander(), p0);
        }

        public Shape modelToView(GlyphView v, int pos, Position.Bias bias, Shape a)
                throws BadLocationException {
            FontMetrics fm = metrics(v);
            Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
            int p0 = v.getStartOffset();
            int p1 = v.getEndOffset();
            if (pos < p0 || pos > p1) {
                throw new BadLocationException("modelToView - can't convert", p1);
            }
            int x = alloc.x;
            if (fm != null && pos > p0) {
                Segment text = v.getText(p0, pos);
                x = x + (int) Utilities.getTabbedTextWidth(text, fm, alloc.x, v.getTabExpander(),
                        p0);
            }
            return new Rectangle(x, alloc.y, 1, alloc.height);
        }

        public int viewToModel(GlyphView v, float x, float y, Shape a,
                Position.Bias[] biasReturn) {
            FontMetrics fm = metrics(v);
            Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
            int p0 = v.getStartOffset();
            int p1 = v.getEndOffset();
            biasReturn[0] = Position.Bias.Forward;
            if (fm == null) {
                return p0;
            }
            Segment text = v.getText(p0, p1);
            int offs = Utilities.getTabbedTextOffset(text, fm, alloc.x, (int) x,
                    v.getTabExpander(), p0);
            return p0 + offs;
        }

        public int getBoundedPosition(GlyphView v, int p0, float x, float len) {
            FontMetrics fm = metrics(v);
            if (fm == null) {
                return p0;
            }
            Segment text = v.getText(p0, v.getEndOffset());
            int offs = Utilities.getTabbedTextOffset(text, fm, x, x + len, v.getTabExpander(),
                    p0, false);
            return p0 + offs;
        }
    }
}
