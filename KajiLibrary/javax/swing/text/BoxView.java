package javax.swing.text;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$ElementChange;

/**
 * Apila a sus hijos sobre un eje: la vista que hace de columna o de fila.
 *
 * <h2>El eje mayor y el menor</h2>
 *
 * <p>Sobre el eje elegido los hijos van uno detras de otro y se reparten el espacio; sobre el otro
 * se encabalgan y se alinean. Es la misma idea de {@code BoxLayout}, y de hecho las cuentas son
 * las mismas: {@link SizeRequirements} las hace para los dos.
 *
 * <p>El maquetado se guarda en cuatro arreglos —desplazamiento y largo por eje— y se recalcula
 * solo cuando algo lo invalida. De ahi los dos pares de banderas: una vista puede tener el eje
 * mayor bien calculado y el menor no, y rehacer solo lo que hace falta es lo que permite que
 * escribir una letra no vuelva a medir el documento entero.
 */
public class BoxView extends CompositeView {

    int majorAxis;
    int majorSpan;
    int minorSpan;

    boolean majorReqValid;
    boolean minorReqValid;
    SizeRequirements majorRequest;
    SizeRequirements minorRequest;

    boolean majorAllocValid;
    int[] majorOffsets;
    int[] majorSpans;

    boolean minorAllocValid;
    int[] minorOffsets;
    int[] minorSpans;

    Rectangle tempRect = new Rectangle();

    /** Una caja sobre ese eje: {@link View#X_AXIS} o {@link View#Y_AXIS}. */
    public BoxView(Element elem, int axis) {
        super(elem);
        tempRect = new Rectangle();
        this.majorAxis = axis;

        majorOffsets = new int[0];
        majorSpans = new int[0];
        majorReqValid = false;
        majorAllocValid = false;
        minorOffsets = new int[0];
        minorSpans = new int[0];
        minorReqValid = false;
        minorAllocValid = false;
    }

    public int getAxis() {
        return majorAxis;
    }

    /** Cambia el eje; invalida todo lo calculado, que dejo de valer. */
    public void setAxis(int axis) {
        boolean axisChanged = (axis != majorAxis);
        majorAxis = axis;
        if (axisChanged) {
            preferenceChanged(null, true, true);
        }
    }

    /** Marca que hay que rehacer el maquetado sobre ese eje. */
    public void layoutChanged(int axis) {
        if (axis == majorAxis) {
            majorAllocValid = false;
        } else {
            minorAllocValid = false;
        }
    }

    protected boolean isLayoutValid(int axis) {
        if (axis == majorAxis) {
            return majorAllocValid;
        }
        return minorAllocValid;
    }

    /** Dibuja un hijo; separado para que una subclase pinte algo alrededor. */
    protected void paintChild(Graphics g, Rectangle alloc, int index) {
        View child = getView(index);
        child.paint(g, alloc);
    }

    public void replace(int index, int length, View[] elems) {
        super.replace(index, length, elems);
        int nInserted = (elems != null) ? elems.length : 0;
        majorOffsets = updateLayoutArray(majorOffsets, index, nInserted);
        majorSpans = updateLayoutArray(majorSpans, index, nInserted);
        majorReqValid = false;
        majorAllocValid = false;
        minorOffsets = updateLayoutArray(minorOffsets, index, nInserted);
        minorSpans = updateLayoutArray(minorSpans, index, nInserted);
        minorReqValid = false;
        minorAllocValid = false;
    }

    /** Agranda o achica un arreglo de maquetado para que siga teniendo un lugar por hijo. */
    int[] updateLayoutArray(int[] oldArray, int offset, int nInserted) {
        int n = getViewCount();
        int[] newArray = new int[n];
        System.arraycopy(oldArray, 0, newArray, 0, offset);
        System.arraycopy(oldArray, offset, newArray, offset + nInserted,
                Math.max(0, oldArray.length - offset));
        return newArray;
    }

    protected void forwardUpdate(DocumentEvent$ElementChange ec, DocumentEvent e, Shape a,
            ViewFactory f) {
        boolean wasValid = isLayoutValid(majorAxis);
        super.forwardUpdate(ec, e, a, f);

        // Si el maquetado se invalido con este aviso, hay que repintar todo lo de abajo.
        if (wasValid && !isLayoutValid(majorAxis)) {
            java.awt.Container c = getContainer();
            if (a != null && c != null) {
                c.repaint();
            }
        }
    }

    /** Un hijo cambio de tamano: hay que rehacer el maquetado del eje que corresponda. */
    public void preferenceChanged(View child, boolean width, boolean height) {
        boolean majorChanged = (majorAxis == X_AXIS) ? width : height;
        boolean minorChanged = (majorAxis == X_AXIS) ? height : width;
        if (majorChanged) {
            majorReqValid = false;
            majorAllocValid = false;
        }
        if (minorChanged) {
            minorReqValid = false;
            minorAllocValid = false;
        }
        super.preferenceChanged(child, width, height);
    }

    /** Se estira sobre el eje menor, no sobre el mayor. */
    public int getResizeWeight(int axis) {
        checkRequests(axis);
        if (axis == majorAxis) {
            if ((majorRequest.preferred != majorRequest.minimum)
                    || (majorRequest.preferred != majorRequest.maximum)) {
                return 1;
            }
        } else {
            if ((minorRequest.preferred != minorRequest.minimum)
                    || (minorRequest.preferred != minorRequest.maximum)) {
                return 1;
            }
        }
        return 0;
    }

    /** Le fija el largo sobre un eje y rehace lo que haga falta. */
    void setSpanOnAxis(int axis, float span) {
        if (axis == majorAxis) {
            if (majorSpan != (int) span) {
                majorAllocValid = false;
            }
            if (!majorAllocValid) {
                majorSpan = (int) span;
                checkRequests(majorAxis);
                layoutMajorAxis(majorSpan, axis, majorOffsets, majorSpans);
                majorAllocValid = true;
                updateChildSizes();
            }
        } else {
            if (((int) span) != minorSpan) {
                minorAllocValid = false;
            }
            if (!minorAllocValid) {
                minorSpan = (int) span;
                checkRequests(axis);
                layoutMinorAxis(minorSpan, axis, minorOffsets, minorSpans);
                minorAllocValid = true;
                updateChildSizes();
            }
        }
    }

    /** Les pasa a los hijos el tamano que les toco. */
    void updateChildSizes() {
        int n = getViewCount();
        if (majorAxis == X_AXIS) {
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                v.setSize((float) majorSpans[i], (float) minorSpans[i]);
            }
        } else {
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                v.setSize((float) minorSpans[i], (float) majorSpans[i]);
            }
        }
    }

    float getSpanOnAxis(int axis) {
        if (axis == majorAxis) {
            return majorSpan;
        }
        return minorSpan;
    }

    public void setSize(float width, float height) {
        layout(Math.max(0, (int) (width - getLeftInset() - getRightInset())),
                Math.max(0, (int) (height - getTopInset() - getBottomInset())));
    }

    /** Dibuja los hijos que caigan dentro del recorte. */
    public void paint(Graphics g, Shape allocation) {
        Rectangle alloc = (allocation instanceof Rectangle) ? (Rectangle) allocation
                : allocation.getBounds();
        int n = getViewCount();
        int x = alloc.x + getLeftInset();
        int y = alloc.y + getTopInset();
        Rectangle clip = g.getClipBounds();
        for (int i = 0; i < n; i++) {
            tempRect.x = x + getOffset(X_AXIS, i);
            tempRect.y = y + getOffset(Y_AXIS, i);
            tempRect.width = getSpan(X_AXIS, i);
            tempRect.height = getSpan(Y_AXIS, i);
            if (clip == null || tempRect.intersects(clip)) {
                paintChild(g, tempRect, i);
            }
        }
    }

    public Shape getChildAllocation(int index, Shape a) {
        if (a != null) {
            Shape ca = super.getChildAllocation(index, a);
            if ((ca != null) && (!isAllocationValid())) {
                // Sin maquetado valido no hay lugar que dar.
                Rectangle r = (ca instanceof Rectangle) ? (Rectangle) ca : ca.getBounds();
                if ((r.width == 0) && (r.height == 0)) {
                    return null;
                }
            }
            return ca;
        }
        return null;
    }

    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        if (!isAllocationValid()) {
            Rectangle alloc = a.getBounds();
            setSize(alloc.width, alloc.height);
        }
        return super.modelToView(pos, a, b);
    }

    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        if (!isAllocationValid()) {
            Rectangle alloc = a.getBounds();
            setSize(alloc.width, alloc.height);
        }
        return super.viewToModel(x, y, a, bias);
    }

    /** La alineacion sale de las cuentas de {@link SizeRequirements}. */
    public float getAlignment(int axis) {
        checkRequests(axis);
        if (axis == majorAxis) {
            return majorRequest.alignment;
        }
        return minorRequest.alignment;
    }

    public float getPreferredSpan(int axis) {
        checkRequests(axis);
        float marginSpan = (axis == X_AXIS) ? getLeftInset() + getRightInset()
                : getTopInset() + getBottomInset();
        if (axis == majorAxis) {
            return ((float) majorRequest.preferred) + marginSpan;
        }
        return ((float) minorRequest.preferred) + marginSpan;
    }

    public float getMinimumSpan(int axis) {
        checkRequests(axis);
        float marginSpan = (axis == X_AXIS) ? getLeftInset() + getRightInset()
                : getTopInset() + getBottomInset();
        if (axis == majorAxis) {
            return ((float) majorRequest.minimum) + marginSpan;
        }
        return ((float) minorRequest.minimum) + marginSpan;
    }

    public float getMaximumSpan(int axis) {
        checkRequests(axis);
        float marginSpan = (axis == X_AXIS) ? getLeftInset() + getRightInset()
                : getTopInset() + getBottomInset();
        if (axis == majorAxis) {
            return ((float) majorRequest.maximum) + marginSpan;
        }
        return ((float) minorRequest.maximum) + marginSpan;
    }

    protected boolean isAllocationValid() {
        return (majorAllocValid && minorAllocValid);
    }

    /** Si ese punto queda antes del principio de la caja, sobre el eje mayor. */
    protected boolean isBefore(int x, int y, Rectangle innerAlloc) {
        if (majorAxis == View.X_AXIS) {
            return (x < innerAlloc.x);
        }
        return (y < innerAlloc.y);
    }

    protected boolean isAfter(int x, int y, Rectangle innerAlloc) {
        if (majorAxis == View.X_AXIS) {
            return (x > (innerAlloc.width + innerAlloc.x));
        }
        return (y > (innerAlloc.height + innerAlloc.y));
    }

    /** El hijo que esta en ese punto, buscando sobre el eje mayor. */
    protected View getViewAtPoint(int x, int y, Rectangle alloc) {
        int n = getViewCount();
        if (majorAxis == View.X_AXIS) {
            if (x < (alloc.x + majorOffsets[0])) {
                childAllocation(0, alloc);
                return getView(0);
            }
            for (int i = 0; i < n; i++) {
                if (x < (alloc.x + majorOffsets[i])) {
                    childAllocation(i - 1, alloc);
                    return getView(i - 1);
                }
            }
            childAllocation(n - 1, alloc);
            return getView(n - 1);
        } else {
            if (y < (alloc.y + majorOffsets[0])) {
                childAllocation(0, alloc);
                return getView(0);
            }
            for (int i = 0; i < n; i++) {
                if (y < (alloc.y + majorOffsets[i])) {
                    childAllocation(i - 1, alloc);
                    return getView(i - 1);
                }
            }
            childAllocation(n - 1, alloc);
            return getView(n - 1);
        }
    }

    protected void childAllocation(int index, Rectangle alloc) {
        alloc.x = alloc.x + getOffset(X_AXIS, index);
        alloc.y = alloc.y + getOffset(Y_AXIS, index);
        alloc.width = getSpan(X_AXIS, index);
        alloc.height = getSpan(Y_AXIS, index);
    }

    /** Rehace el maquetado de los dos ejes para ese tamano. */
    protected void layout(int width, int height) {
        setSpanOnAxis(X_AXIS, width);
        setSpanOnAxis(Y_AXIS, height);
    }

    int getWidth() {
        int span;
        if (majorAxis == X_AXIS) {
            span = majorSpan;
        } else {
            span = minorSpan;
        }
        span = span + getLeftInset() + getRightInset();
        return span;
    }

    int getHeight() {
        int span;
        if (majorAxis == Y_AXIS) {
            span = majorSpan;
        } else {
            span = minorSpan;
        }
        span = span + getTopInset() + getBottomInset();
        return span;
    }

    /**
     * Reparte el eje mayor: los hijos uno detras de otro.
     *
     * <p>La cuenta esta escrita aca y no delegada en {@link SizeRequirements}, aunque el reparto se
     * parezca. La diferencia es el redondeo: aca el ajuste de cada hijo se <em>redondea</em>, y
     * alla se trunca. Con tres hijos y un sobrante de 35 pixeles eso da un pixel de diferencia en
     * el ultimo, que es justo el que tiene que llegar al borde. Truncar deja una franja sin pintar
     * abajo de todo.
     */
    protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
        // Primera pasada: lo que cada hijo prefiere.
        long preferred = 0;
        int n = getViewCount();
        for (int i = 0; i < n; i++) {
            View v = getView(i);
            spans[i] = (int) v.getPreferredSpan(axis);
            preferred = preferred + spans[i];
        }

        // Segunda pasada: estirar o encoger hasta el tamano pedido.
        long desiredAdjustment = targetSpan - preferred;
        float adjustmentFactor = 0.0f;
        int[] diffs = null;

        if (desiredAdjustment != 0) {
            long totalSpan = 0;
            diffs = new int[n];
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                int tmp;
                if (desiredAdjustment < 0) {
                    tmp = (int) v.getMinimumSpan(axis);
                    diffs[i] = spans[i] - tmp;
                } else {
                    tmp = (int) v.getMaximumSpan(axis);
                    diffs[i] = tmp - spans[i];
                }
                totalSpan = totalSpan + tmp;
            }

            float maximumAdjustment = Math.abs(totalSpan - preferred);
            adjustmentFactor = desiredAdjustment / maximumAdjustment;
            adjustmentFactor = Math.min(adjustmentFactor, 1.0f);
            adjustmentFactor = Math.max(adjustmentFactor, -1.0f);
        }

        int totalOffset = 0;
        for (int i = 0; i < n; i++) {
            offsets[i] = totalOffset;
            if (desiredAdjustment != 0) {
                float adjF = adjustmentFactor * diffs[i];
                spans[i] = spans[i] + Math.round(adjF);
            }
            totalOffset = (int) Math.min((long) totalOffset + (long) spans[i],
                    Integer.MAX_VALUE);
        }
    }

    /** Reparte el eje menor: cada hijo toma lo que pueda, alineado. */
    protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
        int n = getViewCount();
        for (int i = 0; i < n; i++) {
            View v = getView(i);
            int max = (int) v.getMaximumSpan(axis);
            if (max < targetSpan) {
                // No llena: se alinea dentro de lo que hay.
                float align = v.getAlignment(axis);
                offsets[i] = (int) ((targetSpan - max) * align);
                spans[i] = max;
            } else {
                int min = (int) v.getMinimumSpan(axis);
                offsets[i] = 0;
                spans[i] = Math.max(min, targetSpan);
            }
        }
    }

    /** Lo que piden los hijos sobre el eje mayor: la suma. */
    protected SizeRequirements calculateMajorAxisRequirements(int axis, SizeRequirements r) {
        int n = getViewCount();
        SizeRequirements[] childRequests = new SizeRequirements[n];
        for (int i = 0; i < n; i++) {
            View v = getView(i);
            childRequests[i] = new SizeRequirements((int) v.getMinimumSpan(axis),
                    (int) v.getPreferredSpan(axis), (int) v.getMaximumSpan(axis),
                    v.getAlignment(axis));
        }
        return SizeRequirements.getTiledSizeRequirements(childRequests);
    }

    /** Lo que piden sobre el eje menor: el maximo, no la suma. */
    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
        int min = 0;
        long pref = 0;
        int max = Integer.MAX_VALUE;
        int n = getViewCount();
        for (int i = 0; i < n; i++) {
            View v = getView(i);
            min = Math.max((int) v.getMinimumSpan(axis), min);
            pref = Math.max((int) v.getPreferredSpan(axis), pref);
            max = Math.max((int) v.getMaximumSpan(axis), max);
        }

        if (r == null) {
            r = new SizeRequirements();
            r.alignment = 0.5f;
        }
        r.preferred = (int) pref;
        r.minimum = min;
        r.maximum = max;
        return r;
    }

    /** Recalcula lo que piden los hijos, si hizo falta. */
    void checkRequests(int axis) {
        if ((axis != X_AXIS) && (axis != Y_AXIS)) {
            throw new IllegalArgumentException("Invalid axis: " + axis);
        }
        if (axis == majorAxis) {
            if (!majorReqValid) {
                majorRequest = calculateMajorAxisRequirements(axis, majorRequest);
                majorReqValid = true;
            }
        } else if (!minorReqValid) {
            minorRequest = calculateMinorAxisRequirements(axis, minorRequest);
            minorReqValid = true;
        }
    }

    /** Reparte alineando por la linea de base; lo usa una fila de texto. */
    protected void baselineLayout(int targetSpan, int axis, int[] offsets, int[] spans) {
        int totalAscent = (int) (targetSpan * getAlignment(axis));
        int totalDescent = targetSpan - totalAscent;
        int n = getViewCount();
        for (int i = 0; i < n; i++) {
            View v = getView(i);
            float align = v.getAlignment(axis);
            int viewSpan;
            if (v.getResizeWeight(axis) > 0) {
                int minSpan = (int) v.getMinimumSpan(axis);
                int maxSpan = (int) v.getMaximumSpan(axis);
                if (align == 0.0f) {
                    viewSpan = Math.max(Math.min(maxSpan, totalDescent), minSpan);
                } else if (align == 1.0f) {
                    viewSpan = Math.max(Math.min(maxSpan, totalAscent), minSpan);
                } else {
                    viewSpan = Math.max(Math.min(maxSpan,
                            Math.min((int) (totalAscent / align),
                                    (int) (totalDescent / (1.0f - align)))), minSpan);
                }
            } else {
                viewSpan = (int) v.getPreferredSpan(axis);
            }
            offsets[i] = totalAscent - (int) (viewSpan * align);
            spans[i] = viewSpan;
        }
    }

    /** Lo que pide una fila alineada por su linea de base. */
    protected SizeRequirements baselineRequirements(int axis, SizeRequirements r) {
        SizeRequirements totalAscent = new SizeRequirements();
        SizeRequirements totalDescent = new SizeRequirements();

        if (r == null) {
            r = new SizeRequirements();
        }
        r.alignment = 0.5f;

        int n = getViewCount();
        for (int i = 0; i < n; i++) {
            View v = getView(i);
            float align = v.getAlignment(axis);
            int span = (int) v.getPreferredSpan(axis);
            int ascent = (int) (align * span);
            int descent = span - ascent;
            totalAscent.preferred = Math.max(ascent, totalAscent.preferred);
            totalDescent.preferred = Math.max(descent, totalDescent.preferred);

            span = (int) v.getMinimumSpan(axis);
            ascent = (int) (align * span);
            descent = span - ascent;
            totalAscent.minimum = Math.max(ascent, totalAscent.minimum);
            totalDescent.minimum = Math.max(descent, totalDescent.minimum);

            span = (int) v.getMaximumSpan(axis);
            ascent = (int) (align * span);
            descent = span - ascent;
            totalAscent.maximum = Math.max(ascent, totalAscent.maximum);
            totalDescent.maximum = Math.max(descent, totalDescent.maximum);
        }
        r.preferred = (int) Math.min((long) totalAscent.preferred + (long) totalDescent.preferred,
                Integer.MAX_VALUE);
        r.minimum = (int) Math.min((long) totalAscent.minimum + (long) totalDescent.minimum,
                Integer.MAX_VALUE);
        r.maximum = (int) Math.min((long) totalAscent.maximum + (long) totalDescent.maximum,
                Integer.MAX_VALUE);
        if (r.preferred > 0) {
            r.alignment = (float) totalAscent.preferred / r.preferred;
        }
        return r;
    }

    /** Donde empieza ese hijo sobre ese eje. */
    protected int getOffset(int axis, int childIndex) {
        int[] offsets = (axis == majorAxis) ? majorOffsets : minorOffsets;
        return offsets[childIndex];
    }

    /** Cuanto mide ese hijo sobre ese eje. */
    protected int getSpan(int axis, int childIndex) {
        int[] spans = (axis == majorAxis) ? majorSpans : minorSpans;
        return spans[childIndex];
    }

    protected boolean flipEastAndWestAtEnds(int position, Position.Bias bias) {
        return false;
    }
}
