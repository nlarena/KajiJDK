package javax.swing.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.event.DocumentEvent;

/**
 * La vista de un documento de texto plano: una linea por elemento, sin vistas hijas.
 *
 * <h2>Sin hijos, a proposito</h2>
 *
 * <p>Un documento de mil lineas tendria mil vistas si cada linea fuera una. Esta clase dibuja las
 * lineas ella misma, recorriendo los elementos: no crea nada por linea. Es lo que hace que un area
 * de texto con un archivo grande no se coma la memoria, y la razon de que herede de {@link View} y
 * no de {@link CompositeView}.
 *
 * <p>El precio es que no puede haber estilos por tramo —todo se dibuja con la fuente del
 * componente— ni lineas de distinto alto. Para eso estan las otras vistas.
 *
 * <h2>La linea mas larga</h2>
 *
 * <p>El ancho preferido es el de la linea mas larga, y encontrarla cuesta recorrer el documento.
 * Se guarda cual era ({@code longLine}) y solo se rehace la busqueda cuando esa linea cambia o
 * aparece una mas larga: es la diferencia entre medir una vez y medir en cada tecla.
 */
public class PlainView extends View implements TabExpander {

    /** Las metricas de la fuente del componente. */
    protected FontMetrics metrics;

    /** La linea mas larga que se conoce; ver la nota de la clase. */
    Element longLine;

    Font font;
    Segment lineBuffer;
    float tabSize;
    int tabBase;

    int sel0;
    int sel1;
    Color unselected;
    Color selected;

    /** Cuanto se corre la primera linea; lo usa {@link FieldView}. */
    int firstLineOffset;

    public PlainView(Element elem) {
        super(elem);
    }

    /** Cuantos espacios ocupa una tabulacion, del documento. */
    protected int getTabSize() {
        Integer i = (Integer) getDocument().getProperty(PlainDocument.tabSizeAttribute);
        int size = (i != null) ? i.intValue() : 8;
        return size;
    }

    /**
     * Dibuja una linea, partida en el tramo seleccionado y los que no.
     *
     * <p>Tres tramos como mucho: lo de antes de la seleccion, la seleccion, y lo de despues. Cada
     * uno se dibuja con su color, y de ahi que haya dos metodos de dibujado.
     */
    protected void drawLine(int lineIndex, Graphics g, int x, int y) {
        Element line = getElement().getElement(lineIndex);
        int p0 = line.getStartOffset();
        int p1 = line.getEndOffset() - 1;
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
            throw new StateInvariantError("Can't render line: " + lineIndex);
        }
    }

    /** Como la anterior, con coordenadas fraccionarias. */
    protected void drawLine(int lineIndex, Graphics2D g, float x, float y) {
        drawLine(lineIndex, (Graphics) g, (int) x, (int) y);
    }

    /** Dibuja un tramo sin seleccionar y devuelve donde termino. */
    protected int drawUnselectedText(Graphics g, int x, int y, int p0, int p1)
            throws BadLocationException {
        g.setColor(unselected);
        Document doc = getDocument();
        Segment s = getLineBuffer();
        doc.getText(p0, p1 - p0, s);
        return Utilities.drawTabbedText(s, x, y, g, this, p0);
    }

    protected float drawUnselectedText(Graphics2D g, float x, float y, int p0, int p1)
            throws BadLocationException {
        return drawUnselectedText((Graphics) g, (int) x, (int) y, p0, p1);
    }

    /** Dibuja un tramo seleccionado. */
    protected int drawSelectedText(Graphics g, int x, int y, int p0, int p1)
            throws BadLocationException {
        g.setColor(selected);
        Document doc = getDocument();
        Segment s = getLineBuffer();
        doc.getText(p0, p1 - p0, s);
        return Utilities.drawTabbedText(s, x, y, g, this, p0);
    }

    float callDrawSelectedText(Graphics g, float x, float y, int p0, int p1)
            throws BadLocationException {
        return drawSelectedText(g, (int) x, (int) y, p0, p1);
    }

    protected float drawSelectedText(Graphics2D g, float x, float y, int p0, int p1)
            throws BadLocationException {
        return drawSelectedText((Graphics) g, (int) x, (int) y, p0, p1);
    }

    /**
     * El segmento de trabajo, uno solo para toda la vista.
     *
     * <p>Se reusa a proposito: dibujar reserva cero memoria, que es lo que hace que desplazar un
     * texto largo no genere basura.
     */
    protected final Segment getLineBuffer() {
        if (lineBuffer == null) {
            lineBuffer = new Segment();
        }
        return lineBuffer;
    }

    /** Vuelve a tomar la fuente del componente; hay que llamarla si cambio. */
    protected void updateMetrics() {
        Component host = getContainer();
        Font f = host.getFont();
        if (font != f) {
            font = f;
            metrics = host.getFontMetrics(f);
            tabSize = getTabSize() * metrics.charWidth('m');
        }
    }

    public float getPreferredSpan(int axis) {
        updateMetrics();
        if (axis == View.X_AXIS) {
            return getLineWidth(getLongLine());
        }
        if (axis == View.Y_AXIS) {
            return getElement().getElementCount() * metrics.getHeight();
        }
        throw new IllegalArgumentException("Invalid axis: " + axis);
    }

    /** La linea mas larga; ver la nota de la clase. */
    private Element getLongLine() {
        if (longLine == null) {
            Element map = getElement();
            int n = map.getElementCount();
            float max = -1;
            for (int i = 0; i < n; i++) {
                Element line = map.getElement(i);
                float w = getLineWidth(line);
                if (w > max) {
                    max = w;
                    longLine = line;
                }
            }
        }
        return longLine;
    }

    /** Cuanto mide esa linea con la fuente actual. */
    private float getLineWidth(Element line) {
        if (line == null) {
            return 0;
        }
        int p0 = line.getStartOffset();
        int p1 = line.getEndOffset() - 1;
        Segment s = getLineBuffer();
        try {
            getDocument().getText(p0, p1 - p0, s);
        } catch (BadLocationException e) {
            return 0;
        }
        return Utilities.getTabbedTextWidth(s, metrics, 0, this, p0);
    }

    /** Dibuja las lineas que caen dentro del recorte, y solo esas. */
    public void paint(Graphics g, Shape a) {
        Shape originalA = a;
        a = adjustPaintRegion(a);
        Rectangle alloc = (Rectangle) a;
        tabBase = alloc.x;
        JTextComponent host = (JTextComponent) getContainer();
        Highlighter h = host.getHighlighter();
        g.setFont(host.getFont());
        updateMetrics();
        selected = host.getSelectedTextColor();
        unselected = host.isEnabled() ? host.getForeground() : host.getDisabledTextColor();
        Caret c = host.getCaret();
        sel0 = (c != null) ? host.getSelectionStart() : 0;
        sel1 = (c != null) ? host.getSelectionEnd() : 0;

        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            clip = alloc;
        }
        int fontHeight = metrics.getHeight();
        int heightBelow = (alloc.y + alloc.height) - (clip.y + clip.height);
        int heightAbove = clip.y - alloc.y;
        int linesBelow = Math.max(0, heightBelow / fontHeight);
        int linesAbove = Math.max(0, heightAbove / fontHeight);
        int linesTotal = (fontHeight > 0) ? alloc.height / fontHeight : 0;

        Element map = getElement();
        int lineCount = map.getElementCount();
        if (alloc.height % fontHeight != 0) {
            linesTotal = linesTotal + 1;
        }
        int line0 = linesAbove;
        int line1 = Math.min(lineCount, linesTotal - linesBelow);

        int y = alloc.y + metrics.getAscent() + (line0 * fontHeight);
        int x = alloc.x;
        for (int line = line0; line < line1; line++) {
            if (h instanceof LayeredHighlighter) {
                Element lineElem = map.getElement(line);
                ((LayeredHighlighter) h).paintLayeredHighlights(g, lineElem.getStartOffset(),
                        lineElem.getEndOffset() - 1, originalA, host, this);
            }
            drawLine(line, g, x, y);
            y = y + fontHeight;
        }
    }

    /** La region donde de verdad se pinta; {@link FieldView} la corre. */
    Shape adjustPaintRegion(Shape a) {
        return a;
    }

    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        updateMetrics();
        Rectangle alloc = a.getBounds();
        Document doc = getDocument();
        Element map = getElement();
        int lineIndex = map.getElementIndex(pos);
        if (lineIndex < 0) {
            return lineToRect(a, 0);
        }
        Rectangle lineArea = lineToRect(a, lineIndex);

        tabBase = lineArea.x;
        Element line = map.getElement(lineIndex);
        int p0 = line.getStartOffset();
        Segment s = getLineBuffer();
        doc.getText(p0, pos - p0, s);
        int xOffs = (int) Utilities.getTabbedTextWidth(s, metrics, tabBase, this, p0);

        lineArea.x = lineArea.x + xOffs;
        lineArea.width = 1;
        lineArea.height = metrics.getHeight();
        return lineArea;
    }

    public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) {
        bias[0] = Position.Bias.Forward;
        Rectangle alloc = a.getBounds();
        Document doc = getDocument();
        int x = (int) fx;
        int y = (int) fy;
        if (y < alloc.y) {
            return getStartOffset();
        }
        if (y > alloc.y + alloc.height) {
            return getEndOffset() - 1;
        }
        Element map = doc.getDefaultRootElement();
        int fontHeight = metrics.getHeight();
        int lineIndex = (fontHeight > 0) ? Math.abs((y - alloc.y) / fontHeight) : 0;
        if (lineIndex >= map.getElementCount()) {
            return getEndOffset() - 1;
        }
        Element line = map.getElement(lineIndex);
        int dx = 0;
        if (lineIndex == 0) {
            alloc.x = alloc.x + firstLineOffset;
            alloc.width = alloc.width - firstLineOffset;
        }
        if (x < alloc.x) {
            return line.getStartOffset();
        }
        int p0 = line.getStartOffset();
        int p1 = line.getEndOffset() - 1;
        Segment s = getLineBuffer();
        try {
            doc.getText(p0, p1 - p0, s);
        } catch (BadLocationException e) {
            throw new StateInvariantError("Can't get line text");
        }
        tabBase = alloc.x;
        int offs = p0 + Utilities.getTabbedTextOffset(s, metrics, tabBase, x, this, p0);
        return Math.min(offs, p1);
    }

    public void insertUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        updateDamage(changes, a, f);
    }

    public void removeUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        updateDamage(changes, a, f);
    }

    public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        updateDamage(changes, a, f);
    }

    public void setSize(float width, float height) {
        super.setSize(width, height);
        updateMetrics();
    }

    /** Donde cae la proxima tabulacion, contando desde el borde de la vista. */
    public float nextTabStop(float x, int tabOffset) {
        if (tabSize == 0) {
            return x;
        }
        int ntabs = (((int) x) - tabBase) / ((int) tabSize);
        return tabBase + ((ntabs + 1) * tabSize);
    }

    /**
     * Marca lo que hay que repintar despues de un cambio.
     *
     * <p>Si cambio la cantidad de lineas, todo lo de abajo se corrio y hay que repintar de ahi
     * para abajo; si cambio una sola linea, alcanza con esa. Esa distincion es la que hace que
     * escribir una letra repinte una linea y no la pantalla.
     */
    protected void updateDamage(DocumentEvent changes, Shape a, ViewFactory f) {
        Component host = getContainer();
        updateMetrics();
        Element elem = getElement();
        javax.swing.event.DocumentEvent$ElementChange ec = changes.getChange(elem);
        Element[] added = (ec != null) ? ec.getChildrenAdded() : null;
        Element[] removed = (ec != null) ? ec.getChildrenRemoved() : null;
        if (((added != null) && (added.length > 0))
                || ((removed != null) && (removed.length > 0))) {
            // Cambio la cantidad de lineas: se rehace todo.
            longLine = null;
            preferenceChanged(null, true, true);
            if (host != null) {
                host.repaint();
            }
        } else {
            Element map = getElement();
            int line = map.getElementIndex(changes.getOffset());
            damageLineRange(line, line, a, host);
            longLine = null;
            preferenceChanged(null, true, false);
        }
    }

    /** Manda repintar el rango de lineas. */
    protected void damageLineRange(int line0, int line1, Shape a, Component host) {
        if (a != null) {
            Rectangle area0 = lineToRect(a, line0);
            Rectangle area1 = lineToRect(a, line1);
            if ((area0 != null) && (area1 != null)) {
                Rectangle damage = area0.union(area1);
                host.repaint(damage.x, damage.y, damage.width, damage.height);
            } else {
                host.repaint();
            }
        }
    }

    /** El rectangulo de esa linea. */
    protected Rectangle lineToRect(Shape a, int line) {
        Rectangle r = null;
        updateMetrics();
        if (metrics != null) {
            Rectangle alloc = a.getBounds();
            if (line == 0) {
                alloc.x = alloc.x + firstLineOffset;
                alloc.width = alloc.width - firstLineOffset;
            }
            r = new Rectangle(alloc.x, alloc.y + (line * metrics.getHeight()), alloc.width,
                    metrics.getHeight());
        }
        return r;
    }
}
