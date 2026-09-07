package javax.swing.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.event.DocumentEvent;

/**
 * Texto plano que se corta en lineas para entrar en el ancho.
 *
 * <h2>Una vista por linea del documento, no por linea visible</h2>
 *
 * <p>Los hijos son una vista por linea <em>del documento</em>; cada una de esas vistas dibuja
 * varias lineas visibles si hace falta. Es la mezcla de las dos ideas: la economia de
 * {@link PlainView}, que no crea nada por linea visible, con el corte de {@link FlowView}.
 *
 * <p>El corte puede ser por palabras o por caracteres, y se elige al construir. Por palabras es lo
 * que uno espera de un editor; por caracteres es lo que hace falta cuando no hay espacios donde
 * cortar.
 */
public class WrappedPlainView extends BoxView implements TabExpander {

    FontMetrics metrics;
    Segment lineBuffer;
    boolean widthChanging;
    int tabBase;
    float tabSize;
    boolean wordWrap;

    int sel0;
    int sel1;
    Color unselected;
    Color selected;

    /** Corta por caracteres. */
    public WrappedPlainView(Element elem) {
        this(elem, false);
    }

    /** Corta por palabras si {@code wordWrap}; ver la nota de la clase. */
    public WrappedPlainView(Element elem, boolean wordWrap) {
        super(elem, Y_AXIS);
        this.wordWrap = wordWrap;
    }

    protected int getTabSize() {
        Integer i = (Integer) getDocument().getProperty(PlainDocument.tabSizeAttribute);
        int size = (i != null) ? i.intValue() : 8;
        return size;
    }

    /** Dibuja un tramo de una linea, partido por la seleccion. */
    protected void drawLine(int p0, int p1, Graphics g, int x, int y) {
        try {
            if (sel0 == sel1 || selected == unselected) {
                drawUnselectedText(g, x, y, p0, p1);
            } else if ((p0 >= sel0 && p0 <= sel1) && (p1 >= sel0 && p1 <= sel1)) {
                drawSelectedText(g, x, y, p0, p1);
            } else if (sel0 >= p0 && sel0 <= p1) {
                if (sel1 >= p0 && sel1 <= p1) {
                    x = drawUnselectedText(g, x, y, p0, sel0);
                    x = drawSelectedText(g, x, y, sel0, sel1);
                    drawUnselectedText(g, x, y, sel1, p1);
                } else {
                    x = drawUnselectedText(g, x, y, p0, sel0);
                    drawSelectedText(g, x, y, sel0, p1);
                }
            } else if (sel1 >= p0 && sel1 <= p1) {
                x = drawSelectedText(g, x, y, p0, sel1);
                drawUnselectedText(g, x, y, sel1, p1);
            } else {
                drawUnselectedText(g, x, y, p0, p1);
            }
        } catch (BadLocationException e) {
            throw new StateInvariantError("Can't render: " + p0 + "," + p1);
        }
    }

    protected void drawLine(int p0, int p1, Graphics2D g, float x, float y) {
        drawLine(p0, p1, (Graphics) g, (int) x, (int) y);
    }

    protected int drawUnselectedText(Graphics g, int x, int y, int p0, int p1)
            throws BadLocationException {
        g.setColor(unselected);
        Document doc = getDocument();
        Segment segment = getLineBuffer();
        doc.getText(p0, p1 - p0, segment);
        return Utilities.drawTabbedText(segment, x, y, g, this, p0);
    }

    protected float drawUnselectedText(Graphics2D g, float x, float y, int p0, int p1)
            throws BadLocationException {
        return drawUnselectedText((Graphics) g, (int) x, (int) y, p0, p1);
    }

    protected int drawSelectedText(Graphics g, int x, int y, int p0, int p1)
            throws BadLocationException {
        g.setColor(selected);
        Document doc = getDocument();
        Segment segment = getLineBuffer();
        doc.getText(p0, p1 - p0, segment);
        return Utilities.drawTabbedText(segment, x, y, g, this, p0);
    }

    protected float drawSelectedText(Graphics2D g, float x, float y, int p0, int p1)
            throws BadLocationException {
        return drawSelectedText((Graphics) g, (int) x, (int) y, p0, p1);
    }

    /** El segmento de trabajo compartido; ver {@link PlainView#getLineBuffer}. */
    protected final Segment getLineBuffer() {
        if (lineBuffer == null) {
            lineBuffer = new Segment();
        }
        return lineBuffer;
    }

    /** Donde hay que cortar para que el tramo entre en el ancho. */
    protected int calculateBreakPosition(int p0, int p1) {
        int p;
        Segment segment = getLineBuffer();
        loadText(segment, p0, p1);
        int currentWidth = getWidth();
        if (wordWrap) {
            p = p0 + Utilities.getBreakLocation(segment, metrics, tabBase, tabBase + currentWidth,
                    this, p0);
        } else {
            p = p0 + Utilities.getTabbedTextOffset(segment, metrics, tabBase,
                    tabBase + currentWidth, this, p0, false);
        }
        return p;
    }

    /** Una vista por linea del documento; ver la nota de la clase. */
    protected void loadChildren(ViewFactory f) {
        Element e = getElement();
        int n = e.getElementCount();
        if (n > 0) {
            View[] added = new View[n];
            for (int i = 0; i < n; i++) {
                added[i] = new WrappedLine(e.getElement(i), this);
            }
            replace(0, 0, added);
        }
    }

    void updateChildren(DocumentEvent e, Shape a) {
        Element elem = getElement();
        javax.swing.event.DocumentEvent$ElementChange ec = e.getChange(elem);
        if (ec != null) {
            Element[] removedElems = ec.getChildrenRemoved();
            Element[] addedElems = ec.getChildrenAdded();
            View[] added = new View[addedElems.length];
            for (int i = 0; i < addedElems.length; i++) {
                added[i] = new WrappedLine(addedElems[i], this);
            }
            replace(ec.getIndex(), removedElems.length, added);
            Container host = getContainer();
            if (host != null) {
                host.repaint();
            }
        }
    }

    /** Carga ese tramo en el segmento de trabajo. */
    final void loadText(Segment segment, int p0, int p1) {
        try {
            Document doc = getDocument();
            doc.getText(p0, p1 - p0, segment);
        } catch (BadLocationException bl) {
            throw new StateInvariantError("Can't get line text");
        }
    }

    final void updateMetrics() {
        Component host = getContainer();
        java.awt.Font f = host.getFont();
        metrics = host.getFontMetrics(f);
        tabSize = getTabSize() * metrics.charWidth('m');
    }

    public float nextTabStop(float x, int tabOffset) {
        if (tabSize == 0) {
            return x;
        }
        int ntabs = (int) ((x - tabBase) / tabSize);
        return tabBase + ((ntabs + 1) * tabSize);
    }

    public void paint(Graphics g, Shape a) {
        Rectangle alloc = (Rectangle) a;
        tabBase = alloc.x;
        JTextComponent host = (JTextComponent) getContainer();
        g.setFont(host.getFont());
        selected = host.getSelectedTextColor();
        unselected = host.isEnabled() ? host.getForeground() : host.getDisabledTextColor();
        Caret c = host.getCaret();
        sel0 = (c != null) ? host.getSelectionStart() : 0;
        sel1 = (c != null) ? host.getSelectionEnd() : 0;
        updateMetrics();
        super.paint(g, a);
    }

    public void setSize(float width, float height) {
        updateMetrics();
        if ((int) width != getWidth()) {
            // Cambio el ancho: hay que volver a cortar todo.
            widthChanging = true;
        }
        super.setSize(width, height);
        widthChanging = false;
    }

    public float getPreferredSpan(int axis) {
        updateMetrics();
        return super.getPreferredSpan(axis);
    }

    public float getMinimumSpan(int axis) {
        updateMetrics();
        return super.getMinimumSpan(axis);
    }

    public float getMaximumSpan(int axis) {
        updateMetrics();
        return super.getMaximumSpan(axis);
    }

    public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        updateChildren(e, a);
        Rectangle alloc = ((a != null) && isAllocationValid()) ? getInsideAllocation(a) : null;
        int pos = e.getOffset();
        View v = getViewAtPosition(pos, alloc);
        if (v != null) {
            v.insertUpdate(e, alloc, f);
        }
    }

    public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        updateChildren(e, a);
        Rectangle alloc = ((a != null) && isAllocationValid()) ? getInsideAllocation(a) : null;
        int pos = e.getOffset();
        View v = getViewAtPosition(pos, alloc);
        if (v != null) {
            v.removeUpdate(e, alloc, f);
        }
    }

    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        updateChildren(e, a);
    }

    /**
     * Una linea del documento, que puede ocupar varias lineas visibles.
     *
     * <p>Es privada en el JDK y aca tambien. No tiene hijos: dibuja los pedazos ella misma,
     * calculando donde cortar cada vez. Es lo que evita crear una vista por linea visible.
     */
    static class WrappedLine extends View {

        private final WrappedPlainView padre;
        private int nlines;

        WrappedLine(Element elem, WrappedPlainView padre) {
            super(elem);
            this.padre = padre;
            nlines = 1;
        }

        /** Cuantas lineas visibles ocupa. */
        final int calculateLineCount() {
            int nlines = 0;
            int startOffset = getStartOffset();
            int p1 = getEndOffset();
            for (int p0 = startOffset; p0 < p1;) {
                nlines = nlines + 1;
                int p = padre.calculateBreakPosition(p0, p1);
                if (p <= p0) {
                    p = p0 + 1;
                }
                p0 = p;
            }
            return nlines;
        }

        public float getPreferredSpan(int axis) {
            if (axis == View.X_AXIS) {
                if (padre.metrics == null) {
                    padre.updateMetrics();
                }
                return padre.getWidth();
            }
            if (padre.metrics == null) {
                padre.updateMetrics();
            }
            nlines = calculateLineCount();
            return nlines * padre.metrics.getHeight();
        }

        public void paint(Graphics g, Shape a) {
            Rectangle alloc = (Rectangle) a;
            int y = alloc.y + padre.metrics.getAscent();
            int x = alloc.x;
            int p1 = getEndOffset();
            for (int p0 = getStartOffset(); p0 < p1;) {
                int p = padre.calculateBreakPosition(p0, p1);
                if (p <= p0) {
                    p = p0 + 1;
                }
                padre.drawLine(p0, Math.min(p, p1 - 1) + ((p >= p1) ? 1 : 0), g, x, y);
                p0 = p;
                y = y + padre.metrics.getHeight();
            }
        }

        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            Rectangle alloc = a.getBounds();
            alloc.height = padre.metrics.getHeight();
            alloc.width = 1;
            int p1 = getEndOffset();
            int p0 = getStartOffset();
            while (p0 < p1) {
                int p = padre.calculateBreakPosition(p0, p1);
                if (p <= p0) {
                    p = p0 + 1;
                }
                if (pos >= p0 && pos < p) {
                    Segment segment = padre.getLineBuffer();
                    padre.loadText(segment, p0, pos);
                    alloc.x = alloc.x + (int) Utilities.getTabbedTextWidth(segment,
                            padre.metrics, alloc.x, padre, p0);
                    return alloc;
                }
                p0 = p;
                alloc.y = alloc.y + alloc.height;
            }
            throw new BadLocationException("Position not in view", pos);
        }

        public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) {
            bias[0] = Position.Bias.Forward;
            Rectangle alloc = a.getBounds();
            int x = (int) fx;
            int y = (int) fy;
            int p1 = getEndOffset();
            int p0 = getStartOffset();
            int lineY = alloc.y;
            int alto = padre.metrics.getHeight();
            while (p0 < p1) {
                int p = padre.calculateBreakPosition(p0, p1);
                if (p <= p0) {
                    p = p0 + 1;
                }
                if (y >= lineY && y < lineY + alto) {
                    Segment segment = padre.getLineBuffer();
                    padre.loadText(segment, p0, p);
                    int offs = Utilities.getTabbedTextOffset(segment, padre.metrics, alloc.x, x,
                            padre, p0);
                    return Math.min(p0 + offs, p1 - 1);
                }
                p0 = p;
                lineY = lineY + alto;
            }
            return p1 - 1;
        }

        public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
            actualizar(a);
        }

        public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
            actualizar(a);
        }

        /** Si cambio la cantidad de lineas visibles, hay que rehacer el maquetado. */
        private void actualizar(Shape a) {
            int n = calculateLineCount();
            if (nlines != n) {
                nlines = n;
                WrappedPlainView wpv = (WrappedPlainView) getParent();
                if (wpv != null) {
                    wpv.preferenceChanged(this, false, true);
                }
            }
            Container c = getContainer();
            if (c != null && a != null) {
                Rectangle alloc = a.getBounds();
                c.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
            }
        }
    }
}
