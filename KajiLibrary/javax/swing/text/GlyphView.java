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
 * Un tramo de texto con una sola fuente y un solo color: la vista que de verdad dibuja letras.
 *
 * <h2>El pintor</h2>
 *
 * <p>La vista no dibuja: le pide a un {@link GlyphPainter}. La separacion parece de mas y no lo es:
 * el mismo tramo se dibuja distinto segun la plataforma —con o sin formas complejas, de izquierda a
 * derecha o al reves— y cambiar de pintor es cambiar todo eso sin tocar la vista. Aca hay un solo
 * pintor, el que mide con {@link FontMetrics} y dibuja con {@code drawChars}.
 *
 * <h2>Fragmentos</h2>
 *
 * <p>Cuando un tramo no entra en una linea, la vista se <em>parte</em>: {@link #breakView} devuelve
 * un fragmento que muestra solo un pedazo del mismo elemento. El fragmento comparte todo con el
 * original salvo dos numeros, {@code offset} y {@code length}, y por eso partir es barato. Esa es
 * la razon de que la clase sea {@code Cloneable}.
 */
public class GlyphView extends View implements TabableView, Cloneable {

    int offset;
    int length;

    /** Si el tramo termina con un fin de linea implicito. */
    boolean impliedCR;

    /** Si el ancho de este tramo no cuenta al medir la linea. */
    boolean skipWidth;

    TabExpander expander;

    /** Donde empieza, para expandir tabulaciones. */
    int x;

    GlyphPainter painter;

    static GlyphPainter defaultPainter;

    /** Una vista de ese elemento, entero. */
    public GlyphView(Element elem) {
        super(elem);
        offset = 0;
        length = 0;
    }

    /** Una copia; la usa {@link #createFragment}. */
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

    /** El texto del tramo, sin copiar cuando se puede. */
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

    /** El color de fondo del tramo, o {@code null} si es transparente. */
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

    /** Quien sabe donde caen las tabulaciones; el padre, si sabe. */
    public TabExpander getTabExpander() {
        return expander;
    }

    /** Se asegura de que haya pintor; lo crea la primera vez. */
    protected void checkPainter() {
        if (painter == null) {
            if (defaultPainter == null) {
                defaultPainter = new PintorSimple();
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

    /** Pinta el fondo si hay, el texto, y las rayas de subrayado o tachado. */
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
            FontMetrics fm = obtenerMetricas();
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

    /** Pinta el tramo con ese color; lo usa quien pinta la seleccion. */
    final void paintTextUsingColor(Graphics g, Shape a, Color c, int p0, int p1) {
        g.setColor(c);
        painter.paint(this, g, a, p0, p1);
    }

    public float getMinimumSpan(int axis) {
        if (axis == View.X_AXIS) {
            checkPainter();
            // El minimo es la palabra mas larga: menos que eso no se puede cortar.
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
        // if/else y no switch: las constantes de View se leen de un `.class` y no se pliegan (#503).
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

    /** Se alinea por su linea de base, no por su caja: es lo que hace que el texto se lea. */
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
     * Que tan bien se corta en ese punto.
     *
     * <p>Excelente si hay un espacio donde cortar, malo si habria que partir una palabra. Es lo
     * que hace que un parrafo corte por espacios.
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

    /** Donde hay un espacio para cortar, mirando de atras para adelante. */
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

    /** Se parte para entrar en ese espacio; devuelve el pedazo que entra. */
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

    /** Un fragmento que muestra ese pedazo; ver la nota de la clase. */
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
        // Cambiaron los atributos: puede haber cambiado la fuente, hay que volver a medir.
        painter = null;
        preferenceChanged(null, true, true);
    }

    void updateAfterChange() {
    }

    /** Datos para justificar el texto; sin justificado, ninguno. */
    JustificationInfo getJustificationInfo(int rowStartOffset) {
        return null;
    }

    /** Las metricas de la fuente de este tramo. */
    private FontMetrics obtenerMetricas() {
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
     * Quien dibuja y mide las letras de una {@link GlyphView}.
     *
     * <p>Sin estado propio: recibe la vista en cada llamada. Por eso un mismo pintor puede servir
     * a miles de tramos, y por eso {@link #getPainter} puede devolverse a si mismo.
     */
    public abstract static class GlyphPainter {

        protected GlyphPainter() {
        }

        /** Cuanto ocupa ese tramo empezando en {@code x}. */
        public abstract float getSpan(GlyphView v, int p0, int p1, TabExpander e, float x);

        public abstract float getHeight(GlyphView v);

        public abstract float getAscent(GlyphView v);

        public abstract float getDescent(GlyphView v);

        public abstract void paint(GlyphView v, Graphics g, Shape a, int p0, int p1);

        public abstract Shape modelToView(GlyphView v, int pos, Position.Bias bias, Shape a)
                throws BadLocationException;

        public abstract int viewToModel(GlyphView v, float x, float y, Shape a,
                Position.Bias[] biasReturn);

        /** Hasta donde llega el texto que entra en {@code len} pixeles. */
        public abstract int getBoundedPosition(GlyphView v, int p0, float x, float len);

        /** El pintor que le corresponde a ese fragmento; por omision, este mismo. */
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

    /** Datos de justificado; sin justificado, no se usa. */
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
     * El pintor de esta biblioteca: mide con {@link FontMetrics} y dibuja con {@code drawChars}.
     *
     * <p>Es el equivalente del {@code GlyphPainter1} del JDK, que es el que se usa cuando el texto
     * no necesita formas complejas. El rasterizador de esta VM no las tiene, asi que este alcanza
     * para todo.
     */
    static class PintorSimple extends GlyphPainter {

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
