package javax.swing.text;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SwingConstants;

/**
 * Las cuentas de texto que comparten todas las vistas: medir, dibujar y buscar limites de palabra.
 *
 * <h2>Por que las tabulaciones complican todo</h2>
 *
 * <p>Sin tabulaciones, medir un texto es sumar anchos de caracteres y se puede hacer de a pedazos.
 * Con tabulaciones no: una tabulacion salta hasta la proxima parada, asi que cuanto ocupa un tramo
 * depende de <em>donde empieza</em>. Por eso todos los metodos de aca reciben la posicion de
 * partida {@code x} y un {@link TabExpander}, y por eso hay tantas variantes.
 *
 * <p>Las tres operaciones son la misma recorrida con distinto final: dibujar, sumar el ancho, o
 * parar cuando se paso de una posicion. Estan separadas porque el dibujado no puede darse el lujo
 * de medir dos veces.
 *
 * <h2>Lo que necesita un componente</h2>
 *
 * <p>{@code getRowStart}, {@code getPositionAbove} y las de palabras trabajan sobre un
 * {@link JTextComponent}: las primeras necesitan su arbol de vistas —donde empieza la <em>fila
 * visible</em> no se puede saber del texto solo— y las de palabras necesitan el idioma del
 * componente. Las de fila devuelven {@code -1} si el componente no tiene aspecto instalado, que
 * en esta biblioteca es siempre: no hay {@code BasicTextUI}.
 */
public class Utilities {

    public Utilities() {
    }

    /** El componente Swing donde vive esa vista, o {@code null}. */
    static javax.swing.JComponent getJComponent(View view) {
        if (view != null) {
            java.awt.Component component = view.getContainer();
            if (component instanceof javax.swing.JComponent) {
                return (javax.swing.JComponent) component;
            }
        }
        return null;
    }

    /**
     * Dibuja el texto expandiendo las tabulaciones; devuelve donde termino.
     *
     * <p>Dibuja de a tramos entre tabulaciones: cada tramo va de una sola vez al contexto grafico,
     * que es mucho mas barato que dibujar caracter por caracter.
     */
    public static final int drawTabbedText(Segment s, int x, int y, Graphics g,
            TabExpander e, int startOffset) {
        return (int) drawTabbedText(null, s, x, y, g, e, startOffset, null);
    }

    /** Como la anterior, con coordenadas fraccionarias. */
    public static final float drawTabbedText(Segment s, float x, float y, Graphics2D g,
            TabExpander e, int startOffset) {
        return drawTabbedText(null, s, x, y, g, e, startOffset, null, true);
    }

    static final int drawTabbedText(View view, Segment s, int x, int y, Graphics g,
            TabExpander e, int startOffset) {
        return (int) drawTabbedText(view, s, x, y, g, e, startOffset, null);
    }

    static final int drawTabbedText(View view, Segment s, int x, int y, Graphics g,
            TabExpander e, int startOffset, int[] justificationData) {
        return (int) drawTabbedText(view, s, x, y, g, e, startOffset, justificationData, true);
    }

    /** La version que hace el trabajo; las demas la llaman. */
    static final float drawTabbedText(View view, Segment s, float x, float y, Graphics g,
            TabExpander e, int startOffset, int[] justificationData, boolean useFPAPI) {
        FontMetrics metrics = g.getFontMetrics();
        float nextX = x;
        char[] txt = s.array;
        int txtOffset = s.offset;
        int flushLen = 0;
        int flushIndex = s.offset;
        int n = s.offset + s.count;
        for (int i = txtOffset; i < n; i++) {
            char c = txt[i];
            if (c == '\t' || c == '\n') {
                if (flushLen > 0) {
                    g.drawChars(txt, flushIndex, flushLen, (int) nextX, (int) y);
                    nextX = nextX + metrics.charsWidth(txt, flushIndex, flushLen);
                    flushLen = 0;
                }
                flushIndex = i + 1;
                if (c == '\t') {
                    if (e != null) {
                        nextX = e.nextTabStop(nextX, startOffset + i - txtOffset);
                    } else {
                        nextX = nextX + metrics.charWidth(' ');
                    }
                }
                // Un fin de linea dentro de un tramo no dibuja nada: la vista ya lo corto.
            } else {
                flushLen = flushLen + 1;
            }
        }
        if (flushLen > 0) {
            g.drawChars(txt, flushIndex, flushLen, (int) nextX, (int) y);
            nextX = nextX + metrics.charsWidth(txt, flushIndex, flushLen);
        }
        return nextX;
    }

    /** Cuanto ocupa ese texto empezando en {@code x}, con las tabulaciones expandidas. */
    public static final int getTabbedTextWidth(Segment s, FontMetrics metrics, int x,
            TabExpander e, int startOffset) {
        return (int) getTabbedTextWidth(null, s, metrics, x, e, startOffset, null);
    }

    public static final float getTabbedTextWidth(Segment s, FontMetrics metrics, float x,
            TabExpander e, int startOffset) {
        return getTabbedTextWidth(null, s, metrics, x, e, startOffset, null);
    }

    static final int getTabbedTextWidth(View view, Segment s, FontMetrics metrics, int x,
            TabExpander e, int startOffset, int[] justificationData) {
        return (int) getTabbedTextWidth(view, s, metrics, (float) x, e, startOffset,
                justificationData);
    }

    static final float getTabbedTextWidth(View view, Segment s, FontMetrics metrics, float x,
            TabExpander e, int startOffset, int[] justificationData) {
        return getTabbedTextWidth(view, s, metrics, x, e, startOffset, justificationData, true);
    }

    /** La version que hace el trabajo. */
    static final float getTabbedTextWidth(View view, Segment s, FontMetrics metrics, float x,
            TabExpander e, int startOffset, int[] justificationData, boolean useFPAPI) {
        float nextX = x;
        char[] txt = s.array;
        int txtOffset = s.offset;
        int n = s.offset + s.count;
        int charCount = 0;
        for (int i = txtOffset; i < n; i++) {
            char c = txt[i];
            if (c == '\t') {
                nextX = nextX + metrics.charsWidth(txt, i - charCount, charCount);
                charCount = 0;
                if (e != null) {
                    nextX = e.nextTabStop(nextX, startOffset + i - txtOffset);
                } else {
                    nextX = nextX + metrics.charWidth(' ');
                }
            } else if (c == '\n') {
                nextX = nextX + metrics.charsWidth(txt, i - charCount, charCount);
                charCount = 0;
            } else {
                charCount = charCount + 1;
            }
        }
        nextX = nextX + metrics.charsWidth(txt, n - charCount, charCount);
        return nextX - x;
    }

    /**
     * Que caracter cae en esa posicion horizontal.
     *
     * <p>Devuelve el desplazamiento dentro del segmento, no del documento. Es la operacion que
     * convierte un clic en una posicion del texto.
     */
    public static final int getTabbedTextOffset(Segment s, FontMetrics metrics, int x0, int x,
            TabExpander e, int startOffset) {
        return getTabbedTextOffset(s, metrics, x0, x, e, startOffset, true);
    }

    static final int getTabbedTextOffset(View view, Segment s, FontMetrics metrics, int x0,
            int x, TabExpander e, int startOffset, int[] justificationData) {
        return getTabbedTextOffset(view, s, metrics, (float) x0, (float) x, e, startOffset, true,
                justificationData, true);
    }

    static final int getTabbedTextOffset(View view, Segment s, FontMetrics metrics, float x0,
            float x, TabExpander e, int startOffset, int[] justificationData) {
        return getTabbedTextOffset(view, s, metrics, x0, x, e, startOffset, true,
                justificationData, true);
    }

    public static final int getTabbedTextOffset(Segment s, FontMetrics metrics, int x0, int x,
            TabExpander e, int startOffset, boolean round) {
        return getTabbedTextOffset(null, s, metrics, (float) x0, (float) x, e, startOffset, round,
                null, true);
    }

    public static final int getTabbedTextOffset(Segment s, FontMetrics metrics, float x0,
            float x, TabExpander e, int startOffset, boolean round) {
        return getTabbedTextOffset(null, s, metrics, x0, x, e, startOffset, round, null, true);
    }

    /**
     * La version que hace el trabajo.
     *
     * <p>{@code round} decide que pasa cuando el punto cae en el medio de un caracter: con
     * {@code true} se elige el borde mas cercano, que es lo que hace que el cursor caiga donde uno
     * apunto y no siempre a la izquierda.
     */
    static final int getTabbedTextOffset(View view, Segment s, FontMetrics metrics, float x0,
            float x, TabExpander e, int startOffset, boolean round, int[] justificationData,
            boolean useFPAPI) {
        if (x0 >= x) {
            return 0;
        }
        float currX = x0;
        float nextX = currX;
        char[] txt = s.array;
        int txtOffset = s.offset;
        int txtCount = s.count;
        int n = s.offset + s.count;
        for (int i = s.offset; i < n; i++) {
            if (txt[i] == '\t') {
                if (e != null) {
                    nextX = e.nextTabStop(nextX, startOffset + i - txtOffset);
                } else {
                    nextX = nextX + metrics.charWidth(' ');
                }
            } else {
                nextX = nextX + metrics.charWidth(txt[i]);
            }
            if (x >= currX && x < nextX) {
                if (round) {
                    if ((x - currX) < (nextX - x)) {
                        return i - txtOffset;
                    }
                    return i + 1 - txtOffset;
                }
                return i - txtOffset;
            }
            currX = nextX;
        }
        return txtCount;
    }

    /**
     * Donde cortar el texto para que entre en ese ancho.
     *
     * <p>Corta en el ultimo espacio antes del limite, no en el caracter exacto: cortar palabras
     * por el medio se ve mal y es lo que distingue este metodo de {@link #getTabbedTextOffset}.
     */
    public static final int getBreakLocation(Segment s, FontMetrics metrics, int x0, int x,
            TabExpander e, int startOffset) {
        return getBreakLocation(s, metrics, (float) x0, (float) x, e, startOffset, true);
    }

    public static final int getBreakLocation(Segment s, FontMetrics metrics, float x0, float x,
            TabExpander e, int startOffset) {
        return getBreakLocation(s, metrics, x0, x, e, startOffset, true);
    }

    static final int getBreakLocation(Segment s, FontMetrics metrics, float x0, float x,
            TabExpander e, int startOffset, boolean useFPAPI) {
        char[] txt = s.array;
        int txtOffset = s.offset;
        int txtCount = s.count;
        int index = getTabbedTextOffset(null, s, metrics, x0, x, e, startOffset, false, null,
                useFPAPI);

        if (index >= txtCount - 1) {
            return txtCount;
        }

        for (int i = txtOffset + index; i >= txtOffset; i--) {
            char ch = txt[i];
            if (ch < 256) {
                if (ch == ' ' || ch == '\t') {
                    // Corta despues del espacio: el espacio se queda en la linea de arriba.
                    index = i - txtOffset + 1;
                    return index;
                }
            } else if (Character.isWhitespace(ch)) {
                index = i - txtOffset + 1;
                return index;
            }
        }
        return index;
    }

    /**
     * Donde empieza la fila visible que contiene esa posicion.
     *
     * <p>{@code -1} sin aspecto instalado; ver la nota de la clase.
     */
    public static final int getRowStart(JTextComponent c, int offs) throws BadLocationException {
        Rectangle r = ubicacion(c, offs);
        if (r == null) {
            return -1;
        }
        int lastOffs = offs;
        int y = r.y;
        while ((r != null) && (y == r.y)) {
            offs = lastOffs;
            lastOffs = lastOffs - 1;
            r = (lastOffs >= 0) ? ubicacion(c, lastOffs) : null;
        }
        return offs;
    }

    /** Donde termina la fila visible que contiene esa posicion. */
    public static final int getRowEnd(JTextComponent c, int offs) throws BadLocationException {
        Rectangle r = ubicacion(c, offs);
        if (r == null) {
            return -1;
        }
        int n = c.getDocument().getLength();
        int lastOffs = offs;
        int y = r.y;
        while ((r != null) && (y == r.y)) {
            offs = lastOffs;
            lastOffs = lastOffs + 1;
            r = (lastOffs <= n) ? ubicacion(c, lastOffs) : null;
        }
        return offs;
    }

    /** Donde cae esa posicion, o {@code null} si el componente no tiene aspecto. */
    private static Rectangle ubicacion(JTextComponent c, int offs) throws BadLocationException {
        javax.swing.plaf.TextUI ui = c.getUI();
        if (ui == null) {
            return null;
        }
        return ui.modelToView(c, offs);
    }

    /** La posicion que queda justo arriba, a la misma altura horizontal. */
    public static final int getPositionAbove(JTextComponent c, int offs, int x)
            throws BadLocationException {
        return getPositionAbove(c, offs, (float) x, true);
    }

    public static final int getPositionAbove(JTextComponent c, int offs, float x)
            throws BadLocationException {
        return getPositionAbove(c, offs, x, true);
    }

    static final int getPositionAbove(JTextComponent c, int offs, float x, boolean useFPAPI)
            throws BadLocationException {
        int lastOffs = getRowStart(c, offs) - 1;
        if (lastOffs < 0) {
            return -1;
        }
        return posicionEnFila(c, lastOffs, x);
    }

    public static final int getPositionBelow(JTextComponent c, int offs, int x)
            throws BadLocationException {
        return getPositionBelow(c, offs, (float) x, true);
    }

    public static final int getPositionBelow(JTextComponent c, int offs, float x)
            throws BadLocationException {
        return getPositionBelow(c, offs, x, true);
    }

    static final int getPositionBelow(JTextComponent c, int offs, float x, boolean useFPAPI)
            throws BadLocationException {
        int lastOffs = getRowEnd(c, offs) + 1;
        if (lastOffs <= 0 || lastOffs > c.getDocument().getLength()) {
            return -1;
        }
        return posicionEnFila(c, lastOffs, x);
    }

    /** La posicion de esa fila que queda mas cerca de esa columna. */
    private static int posicionEnFila(JTextComponent c, int offsEnFila, float x)
            throws BadLocationException {
        int inicio = getRowStart(c, offsEnFila);
        int fin = getRowEnd(c, offsEnFila);
        if (inicio < 0 || fin < 0) {
            return -1;
        }
        int mejor = inicio;
        float mejorDist = Float.MAX_VALUE;
        for (int i = inicio; i <= fin; i++) {
            Rectangle r = ubicacion(c, i);
            if (r != null) {
                float d = Math.abs(r.x - x);
                if (d < mejorDist) {
                    mejorDist = d;
                    mejor = i;
                }
            }
        }
        return mejor;
    }

    /** Donde empieza la palabra que contiene esa posicion. */
    public static final int getWordStart(JTextComponent c, int offs) throws BadLocationException {
        Document doc = c.getDocument();
        Element line = getParagraphElement(c, offs);
        if (line == null) {
            throw new BadLocationException("No word at " + offs, offs);
        }
        int lineStart = line.getStartOffset();
        int lineEnd = Math.min(line.getEndOffset(), doc.getLength());
        Segment seg = new Segment();
        doc.getText(lineStart, lineEnd - lineStart, seg);
        if (seg.count > 0) {
            int i = offs - lineStart;
            if (i >= seg.count) {
                i = seg.count - 1;
            }
            while (i > 0 && !esSeparador(seg.array[seg.offset + i - 1])) {
                i = i - 1;
            }
            return lineStart + i;
        }
        return offs;
    }

    /** Donde termina la palabra que contiene esa posicion. */
    public static final int getWordEnd(JTextComponent c, int offs) throws BadLocationException {
        Document doc = c.getDocument();
        Element line = getParagraphElement(c, offs);
        if (line == null) {
            throw new BadLocationException("No word at " + offs, offs);
        }
        int lineStart = line.getStartOffset();
        int lineEnd = Math.min(line.getEndOffset(), doc.getLength());
        Segment seg = new Segment();
        doc.getText(lineStart, lineEnd - lineStart, seg);
        if (seg.count > 0) {
            int i = offs - lineStart;
            while (i < seg.count && !esSeparador(seg.array[seg.offset + i])) {
                i = i + 1;
            }
            return lineStart + i;
        }
        return offs;
    }

    /** El principio de la palabra siguiente. */
    public static final int getNextWord(JTextComponent c, int offs) throws BadLocationException {
        Document doc = c.getDocument();
        int n = doc.getLength();
        Segment seg = new Segment();
        doc.getText(offs, n - offs, seg);
        boolean vistoSeparador = false;
        for (int i = 0; i < seg.count; i++) {
            char ch = seg.array[seg.offset + i];
            if (esSeparador(ch)) {
                vistoSeparador = true;
            } else if (vistoSeparador) {
                return offs + i;
            }
        }
        throw new BadLocationException("No more words", offs);
    }

    static int getNextWordInParagraph(JTextComponent c, Element line, int offs, boolean first)
            throws BadLocationException {
        return getNextWord(c, offs);
    }

    /** El principio de la palabra anterior. */
    public static final int getPreviousWord(JTextComponent c, int offs)
            throws BadLocationException {
        if (offs <= 0) {
            throw new BadLocationException("No more words", offs);
        }
        Document doc = c.getDocument();
        Segment seg = new Segment();
        doc.getText(0, offs, seg);
        int i = seg.count - 1;
        while (i >= 0 && esSeparador(seg.array[seg.offset + i])) {
            i = i - 1;
        }
        while (i > 0 && !esSeparador(seg.array[seg.offset + i - 1])) {
            i = i - 1;
        }
        if (i < 0) {
            throw new BadLocationException("No more words", offs);
        }
        return i;
    }

    static int getPrevWordInParagraph(JTextComponent c, Element line, int offs)
            throws BadLocationException {
        return getPreviousWord(c, offs);
    }

    /** Que separa una palabra de otra. */
    private static boolean esSeparador(char ch) {
        return Character.isWhitespace(ch) || (!Character.isLetterOrDigit(ch) && ch != '_');
    }

    /** El parrafo que contiene esa posicion, segun el documento del componente. */
    public static final Element getParagraphElement(JTextComponent c, int offs) {
        Document doc = c.getDocument();
        if (doc instanceof StyledDocument) {
            return ((StyledDocument) doc).getParagraphElement(offs);
        }
        Element map = doc.getDefaultRootElement();
        int index = map.getElementIndex(offs);
        Element line = map.getElement(index);
        if ((offs >= line.getStartOffset()) && (offs < line.getEndOffset())) {
            return line;
        }
        return null;
    }

    /** Si ese tramo es texto en composicion de un metodo de entrada; nunca, aca. */
    static boolean isComposedTextElement(Document doc, int offset) {
        return false;
    }

    static boolean isComposedTextElement(Element elem) {
        return false;
    }

    static boolean isComposedTextAttributeDefined(AttributeSet as) {
        return false;
    }

    /** Sin metodos de entrada no hay texto en composicion que dibujar. */
    static int drawComposedText(View view, AttributeSet attr, Graphics g, int x, int y, int p0,
            int p1) throws BadLocationException {
        return x;
    }

    static float drawComposedText(View view, AttributeSet attr, Graphics g, float x, float y,
            int p0, int p1) throws BadLocationException {
        return x;
    }

    static float drawComposedText(View view, AttributeSet attr, Graphics g, float x, float y,
            int p0, int p1, boolean useFPAPI) throws BadLocationException {
        return x;
    }

    static void paintComposedText(Graphics g, Rectangle alloc, GlyphView v) {
    }

    /** Si ese componente se lee de izquierda a derecha. */
    static boolean isLeftToRight(java.awt.Component c) {
        return c.getComponentOrientation().isLeftToRight();
    }

    /**
     * La proxima posicion visual dentro de una vista con hijos.
     *
     * <p>Le pide al hijo que tiene la posicion y, si se le acaba, al de al lado; ver
     * {@link CompositeView}.
     */
    static int getNextVisualPositionFrom(View v, int pos, Position.Bias b, Shape alloc,
            int direction, Position.Bias[] biasRet) throws BadLocationException {
        if (v.getViewCount() == 0) {
            return pos;
        }
        boolean top = (direction == SwingConstants.NORTH || direction == SwingConstants.WEST);
        int retValue;
        if (pos == -1) {
            int childIndex = top ? v.getViewCount() - 1 : 0;
            View child = v.getView(childIndex);
            Shape childBounds = v.getChildAllocation(childIndex, alloc);
            retValue = child.getNextVisualPositionFrom(pos, b, childBounds, direction, biasRet);
        } else {
            int increment = top ? -1 : 1;
            int childIndex;
            if (b == Position.Bias.Backward && pos > 0) {
                childIndex = v.getViewIndex(pos - 1, Position.Bias.Forward);
            } else {
                childIndex = v.getViewIndex(pos, Position.Bias.Forward);
            }
            if (childIndex < 0) {
                return pos;
            }
            View child = v.getView(childIndex);
            Shape childBounds = v.getChildAllocation(childIndex, alloc);
            retValue = child.getNextVisualPositionFrom(pos, b, childBounds, direction, biasRet);
            childIndex = childIndex + increment;
            if (retValue == -1 && childIndex >= 0 && childIndex < v.getViewCount()) {
                child = v.getView(childIndex);
                childBounds = v.getChildAllocation(childIndex, alloc);
                retValue = child.getNextVisualPositionFrom(-1, b, childBounds, direction,
                        biasRet);
            }
        }
        return retValue;
    }
}
