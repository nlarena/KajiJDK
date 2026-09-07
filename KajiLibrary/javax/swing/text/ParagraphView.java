package javax.swing.text;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;

/**
 * Un parrafo: filas de texto con sangria, interlineado, alineacion y tabulaciones.
 *
 * <h2>Lo que agrega sobre {@link FlowView}</h2>
 *
 * <p>Flow sabe cortar en filas. Este sabe <em>como se ve</em> un parrafo: la primera linea puede
 * ir sangrada distinto, las lineas pueden ir separadas, el texto puede ir centrado o a la derecha,
 * y hay paradas de tabulacion. Todo eso sale de los atributos de parrafo, y por eso
 * {@link #setPropertiesFromAttributes} es el metodo central.
 *
 * <p>Es {@link TabExpander} porque las paradas son del parrafo: una tabulacion en medio de una
 * fila pregunta a su parrafo, no a la fila.
 */
public class ParagraphView extends FlowView implements TabExpander {

    /** Cuanto se sangra la primera linea, en pixeles. */
    protected int firstLineIndent = 0;

    static Class<?> i18nStrategy;

    /** Los caracteres que cortan en una tabulacion decimal. */
    static char[] tabChars = {'\t'};

    static char[] tabDecimalChars = {'\t', '.'};

    private int justification;
    private float lineSpacing;
    private TabSet tabSet;

    /** Un parrafo de ese elemento, apilando filas hacia abajo. */
    public ParagraphView(Element elem) {
        super(elem, View.Y_AXIS);
        setPropertiesFromAttributes();
    }

    protected void setJustification(int j) {
        justification = j;
    }

    protected void setLineSpacing(float ls) {
        lineSpacing = ls;
    }

    protected void setFirstLineIndent(float fi) {
        firstLineIndent = (int) fi;
    }

    /** Toma sangrias, interlineado, alineacion y tabulaciones de los atributos. */
    protected void setPropertiesFromAttributes() {
        AttributeSet attr = getAttributes();
        if (attr != null) {
            setParagraphInsets(attr);
            setJustification(StyleConstants.getAlignment(attr));
            setLineSpacing(StyleConstants.getLineSpacing(attr));
            setFirstLineIndent(StyleConstants.getFirstLineIndent(attr));
            tabSet = StyleConstants.getTabSet(attr);
        }
    }

    /** Cuantas vistas hay en el arbol logico. */
    protected int getLayoutViewCount() {
        return layoutPool.getViewCount();
    }

    protected View getLayoutView(int index) {
        return layoutPool.getView(index);
    }

    /** Subir o bajar una linea dentro del parrafo. */
    protected int getNextNorthSouthVisualPositionFrom(int pos, Position.Bias b, Shape a,
            int direction, Position.Bias[] biasRet) throws BadLocationException {
        int vIndex;
        if (pos == -1) {
            vIndex = (direction == NORTH) ? getViewCount() - 1 : 0;
        } else {
            if (b == Position.Bias.Backward && pos > 0) {
                vIndex = getViewIndexAtPosition(pos - 1);
            } else {
                vIndex = getViewIndexAtPosition(pos);
            }
            if (direction == NORTH) {
                vIndex = vIndex - 1;
            } else {
                vIndex = vIndex + 1;
            }
        }
        if (vIndex < 0 || vIndex >= getViewCount()) {
            return -1;
        }
        int x = 0;
        try {
            Shape s = modelToView(pos < 0 ? getStartOffset() : pos, a, Position.Bias.Forward);
            if (s != null) {
                x = s.getBounds().x;
            }
        } catch (BadLocationException e) {
            x = 0;
        }
        return getClosestPositionTo(pos, b, a, direction, biasRet, vIndex, x);
    }

    /** La posicion de esa fila que queda mas cerca de esa columna. */
    protected int getClosestPositionTo(int pos, Position.Bias b, Shape a, int direction,
            Position.Bias[] biasRet, int rowIndex, int x) throws BadLocationException {
        View row = getView(rowIndex);
        Shape rowAlloc = getChildAllocation(rowIndex, a);
        biasRet[0] = Position.Bias.Forward;
        if (rowAlloc == null) {
            return row.getStartOffset();
        }
        return row.viewToModel(x, rowAlloc.getBounds().y, rowAlloc, biasRet);
    }

    protected boolean flipEastAndWestAtEnds(int position, Position.Bias bias) {
        return false;
    }

    /** La primera fila puede tener menos lugar, por la sangria. */
    public int getFlowSpan(int index) {
        View row = getView(index);
        int adjust = 0;
        if (row != null && index == 0) {
            adjust = adjust + firstLineIndent;
        }
        return (layoutSpan == Integer.MAX_VALUE) ? layoutSpan : layoutSpan - adjust;
    }

    public int getFlowStart(int index) {
        if (index == 0) {
            return firstLineIndent;
        }
        return 0;
    }

    /** Una fila; es una caja horizontal con la alineacion del parrafo. */
    protected View createRow() {
        return new Row(getElement(), this);
    }

    /** Donde cae la proxima tabulacion, segun las paradas del parrafo. */
    public float nextTabStop(float x, int tabOffset) {
        if (tabSet == null) {
            // Sin paradas propias: cada media pulgada, como el JDK.
            float tabBase = getTabBase();
            float defaultAncho = 36;
            int ntabs = (int) ((x - tabBase) / defaultAncho);
            return tabBase + ((ntabs + 1) * defaultAncho);
        }
        float tabBase = getTabBase();
        TabStop tab = tabSet.getTabAfter(x - tabBase + 0.01f);
        if (tab == null) {
            return tabBase + ((int) ((x - tabBase) / 36) + 1) * 36;
        }
        int alignment = tab.getAlignment();
        if (alignment == TabStop.ALIGN_LEFT || alignment == TabStop.ALIGN_BAR) {
            return tabBase + tab.getPosition();
        }
        // Las demas alineaciones necesitan medir lo que viene: se mide hasta el proximo corte.
        int p = getPartialLength(tabOffset, alignment);
        float tabPos = tabBase + tab.getPosition();
        if (alignment == TabStop.ALIGN_RIGHT || alignment == TabStop.ALIGN_DECIMAL) {
            return Math.max(x, tabPos - p);
        }
        return Math.max(x, tabPos - (p / 2));
    }

    /** Cuanto ocupa lo que sigue hasta el proximo corte, para alinear una tabulacion. */
    private int getPartialLength(int tabOffset, int alignment) {
        char[] cortes = (alignment == TabStop.ALIGN_DECIMAL) ? tabDecimalChars : tabChars;
        int hasta = findOffsetToCharactersInString(cortes, tabOffset + 1);
        if (hasta == -1) {
            hasta = getEndOffset();
        }
        return (int) getPartialSize(tabOffset + 1, hasta);
    }

    protected TabSet getTabSet() {
        return StyleConstants.getTabSet(getElement().getAttributes());
    }

    /** Cuanto ocupa ese tramo del parrafo. */
    protected float getPartialSize(int startOffset, int endOffset) {
        float size = 0;
        int viewIndex;
        int numViews = getViewCount();
        View view;
        int viewEnd;
        int tempEnd;

        viewIndex = getElement().getElementIndex(startOffset);
        numViews = getLayoutViewCount();
        while (startOffset < endOffset && viewIndex < numViews) {
            view = getLayoutView(viewIndex);
            viewEnd = view.getEndOffset();
            tempEnd = Math.min(endOffset, viewEnd);
            if (view instanceof TabableView) {
                size = size + ((TabableView) view).getPartialSpan(startOffset, tempEnd);
            } else if (startOffset == view.getStartOffset() && tempEnd == viewEnd) {
                size = size + view.getPreferredSpan(View.X_AXIS);
            }
            startOffset = viewEnd;
            viewIndex = viewIndex + 1;
        }
        return size;
    }

    /** Donde esta el primero de esos caracteres a partir de esa posicion. */
    protected int findOffsetToCharactersInString(char[] string, int start) {
        int stringLength = string.length;
        int end = getEndOffset();
        Segment seg = new Segment();
        try {
            getDocument().getText(start, end - start, seg);
        } catch (BadLocationException ble) {
            return -1;
        }
        int maxCounter = seg.offset + seg.count;
        for (int counter = seg.offset; counter < maxCounter; counter++) {
            char currentChar = seg.array[counter];
            for (int subCounter = 0; subCounter < stringLength; subCounter++) {
                if (currentChar == string[subCounter]) {
                    return counter - seg.offset + start;
                }
            }
        }
        return -1;
    }

    /** Desde donde se cuentan las tabulaciones: el borde izquierdo del parrafo. */
    protected float getTabBase() {
        return (float) (getLeftInset() + firstLineIndent);
    }

    public void paint(Graphics g, Shape a) {
        Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
        super.paint(g, a);
    }

    /** Un parrafo se alinea por arriba en vertical. */
    public float getAlignment(int axis) {
        if (axis == View.Y_AXIS) {
            return 0;
        }
        return 0.5f;
    }

    /** Un parrafo no se parte: sus filas ya son el corte. */
    public View breakView(int axis, float len, Shape a) {
        if (axis == View.Y_AXIS) {
            if (a != null) {
                Rectangle alloc = a.getBounds();
                setSize(alloc.width, alloc.height);
            }
            return this;
        }
        return this;
    }

    public int getBreakWeight(int axis, float len) {
        return BadBreakWeight;
    }

    /** El minimo horizontal es el de la palabra mas larga. */
    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
        r = super.calculateMinorAxisRequirements(axis, r);
        float insets = getLeftInset() + getRightInset();
        r.minimum = (int) (r.minimum + insets + firstLineIndent);
        r.preferred = (int) (r.preferred + insets + firstLineIndent);
        r.maximum = Integer.MAX_VALUE;
        return r;
    }

    /** Cambiaron los atributos: hay que volver a leer sangrias y alineacion. */
    public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        setPropertiesFromAttributes();
        layoutChanged(X_AXIS);
        layoutChanged(Y_AXIS);
        super.changedUpdate(changes, a, f);
    }

    /**
     * Una fila del parrafo: una caja horizontal.
     *
     * <p>Es privada en el JDK y aca tambien. Su alineacion horizontal es la del parrafo, y de ahi
     * que un parrafo centrado tenga las filas centradas sin que la fila sepa nada.
     */
    static class Row extends BoxView {

        private final ParagraphView parrafo;

        Row(Element elem, ParagraphView parrafo) {
            super(elem, View.X_AXIS);
            this.parrafo = parrafo;
        }

        /** Una fila no tiene margenes propios: los del parrafo ya se aplicaron. */
        protected void loadChildren(ViewFactory f) {
        }

        public AttributeSet getAttributes() {
            View p = getParent();
            return (p != null) ? p.getAttributes() : null;
        }

        public float getAlignment(int axis) {
            if (axis == View.X_AXIS) {
                int j = (parrafo != null) ? parrafo.justification : StyleConstants.ALIGN_LEFT;
                if (j == StyleConstants.ALIGN_LEFT) {
                    return 0;
                }
                if (j == StyleConstants.ALIGN_RIGHT) {
                    return 1;
                }
                if (j == StyleConstants.ALIGN_CENTER) {
                    return 0.5f;
                }
                return 0;
            }
            return super.getAlignment(axis);
        }

        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            Rectangle r = a.getBounds();
            View v = getViewAtPosition(pos, r);
            if ((v != null) && (!v.getElement().isLeaf())) {
                // El fin de linea de una fila no se dibuja: la posicion cae al final.
                return super.modelToView(pos, a, b);
            }
            r = a.getBounds();
            int height = r.height;
            int y = r.y;
            Shape loc = super.modelToView(pos, a, b);
            r = loc.getBounds();
            r.height = height;
            r.y = y;
            return r;
        }

        public int getStartOffset() {
            int offs = Integer.MAX_VALUE;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                offs = Math.min(offs, v.getStartOffset());
            }
            return offs;
        }

        public int getEndOffset() {
            int offs = 0;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                offs = Math.max(offs, v.getEndOffset());
            }
            return offs;
        }

        /** Las filas se alinean por su linea de base. */
        protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            baselineLayout(targetSpan, axis, offsets, spans);
        }

        protected SizeRequirements calculateMinorAxisRequirements(int axis,
                SizeRequirements r) {
            return baselineRequirements(axis, r);
        }

        protected int getViewIndexAtPosition(int pos) {
            if (pos < getStartOffset() || pos >= getEndOffset()) {
                return -1;
            }
            for (int counter = getViewCount() - 1; counter >= 0; counter--) {
                View v = getView(counter);
                if (pos >= v.getStartOffset() && pos < v.getEndOffset()) {
                    return counter;
                }
            }
            return -1;
        }
    }
}
