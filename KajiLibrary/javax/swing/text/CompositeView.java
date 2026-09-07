package javax.swing.text;

import java.awt.Rectangle;
import java.awt.Shape;

/**
 * Una vista con hijos: la base de todo lo que agrupa.
 *
 * <h2>Lo que resuelve, y lo que deja abierto</h2>
 *
 * <p>Guarda los hijos, los crea a partir de los elementos del documento ({@link #loadChildren}) y
 * traduce entre modelo y pantalla delegando en el hijo que corresponde. Lo que <em>no</em> decide
 * es como estan puestos: eso lo contestan {@link #childAllocation}, {@link #isBefore} y
 * {@link #isAfter}, que cada subclase implementa. Poner los hijos en fila, en columna o en una
 * grilla es la unica diferencia entre las subclases.
 *
 * <h2>Los margenes</h2>
 *
 * <p>Los cuatro insets son {@code short} y no {@code int}: una vista puede haber miles en un
 * documento y cada campo cuenta. {@link #getInsideAllocation} es la que descuenta los margenes y
 * deja el rectangulo donde de verdad van los hijos.
 *
 * <h2>Moverse con las flechas</h2>
 *
 * <p>{@link #getNextVisualPositionFrom} se parte en dos: arriba y abajo lo resuelve el padre
 * —hay que cambiar de hijo—, izquierda y derecha lo resuelve el hijo mientras pueda. Esa division
 * es la que hace que bajar una linea funcione igual en un parrafo que en una tabla.
 */
public abstract class CompositeView extends View {

    private View[] children;
    private int nchildren;
    private short left;
    private short right;
    private short top;
    private short bottom;
    private Rectangle childAlloc;

    /** Una vista de ese elemento, sin hijos todavia. */
    public CompositeView(Element elem) {
        super(elem);
        children = new View[1];
        nchildren = 0;
        childAlloc = new Rectangle();
    }

    /**
     * Crea las vistas hijas, una por elemento hijo.
     *
     * <p>Se llama sola la primera vez que la vista tiene padre: hasta ese momento no hay fabrica
     * a quien pedirselas.
     */
    protected void loadChildren(ViewFactory f) {
        if (f == null) {
            return;
        }
        Element e = getElement();
        int n = e.getElementCount();
        if (n > 0) {
            View[] added = new View[n];
            for (int i = 0; i < n; i++) {
                added[i] = f.create(e.getElement(i));
            }
            replace(0, 0, added);
        }
    }

    /** Al tener padre por primera vez, carga los hijos. */
    public void setParent(View parent) {
        super.setParent(parent);
        if ((parent != null) && (nchildren == 0)) {
            ViewFactory f = getViewFactory();
            loadChildren(f);
        }
    }

    public int getViewCount() {
        return nchildren;
    }

    public View getView(int n) {
        return children[n];
    }

    /** Cambia un tramo de hijos por otro; a los que se van les saca el padre. */
    public void replace(int offset, int length, View[] views) {
        if (views == null) {
            views = new View[0];
        }

        for (int i = offset; i < offset + length; i++) {
            if (children[i].getParent() == this) {
                children[i].setParent(null);
            }
            children[i] = null;
        }

        int delta = views.length - length;
        int src = offset + length;
        int nmove = nchildren - src;
        int dest = src + delta;
        if ((nchildren + delta) >= children.length) {
            int newLength = Math.max(2 * children.length, nchildren + delta);
            View[] newChildren = new View[newLength];
            System.arraycopy(children, 0, newChildren, 0, offset);
            System.arraycopy(views, 0, newChildren, offset, views.length);
            System.arraycopy(children, src, newChildren, dest, nmove);
            children = newChildren;
        } else {
            System.arraycopy(children, src, children, dest, nmove);
            System.arraycopy(views, 0, children, offset, views.length);
        }
        nchildren = nchildren + delta;

        for (int i = 0; i < views.length; i++) {
            views[i].setParent(this);
        }
    }

    /** El lugar de ese hijo, ya descontados los margenes. */
    public Shape getChildAllocation(int index, Shape a) {
        Rectangle alloc = getInsideAllocation(a);
        childAllocation(index, alloc);
        return alloc;
    }

    /** Donde cae esa posicion: en el hijo que la contiene. */
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        boolean isBackward = (b == Position.Bias.Backward);
        int testPos = (isBackward) ? Math.max(0, pos - 1) : pos;
        if (isBackward && testPos < getStartOffset()) {
            return null;
        }
        int vIndex = getViewIndexAtPosition(testPos);
        if ((vIndex != -1) && (vIndex < getViewCount())) {
            View v = getView(vIndex);
            if (v != null && testPos >= v.getStartOffset() && testPos < v.getEndOffset()) {
                Shape childShape = getChildAllocation(vIndex, a);
                if (childShape == null) {
                    return null;
                }
                Shape retShape = v.modelToView(pos, childShape, b);
                if (retShape == null && v.getEndOffset() == pos) {
                    // El final de un hijo es el principio del siguiente.
                    if (++vIndex < getViewCount()) {
                        v = getView(vIndex);
                        retShape = v.modelToView(pos, getChildAllocation(vIndex, a), b);
                    }
                }
                return retShape;
            }
        }
        throw new BadLocationException("Position not represented by view", pos);
    }

    /** La region de las dos posiciones; si caen en el mismo hijo se la pide a el. */
    public Shape modelToView(int p0, Position.Bias b0, int p1, Position.Bias b1, Shape a)
            throws BadLocationException {
        if (p0 == getStartOffset() && p1 == getEndOffset()) {
            return a;
        }
        Rectangle alloc = getInsideAllocation(a);
        Rectangle r0 = new Rectangle(alloc);
        View v0 = getViewAtPosition((b0 == Position.Bias.Backward) ? Math.max(0, p0 - 1) : p0, r0);
        Rectangle r1 = new Rectangle(alloc);
        View v1 = getViewAtPosition((b1 == Position.Bias.Backward) ? Math.max(0, p1 - 1) : p1, r1);
        if (v0 == v1) {
            if (v0 == null) {
                return a;
            }
            return v0.modelToView(p0, b0, p1, b1, r0);
        }
        // En hijos distintos: la union de los dos lugares.
        Shape r = (v0 != null) ? v0.modelToView(p0, b0, v0.getEndOffset(),
                Position.Bias.Backward, r0) : a;
        Rectangle rr = (r instanceof Rectangle) ? (Rectangle) r : r.getBounds();
        if (v1 != null) {
            Shape r1s = v1.modelToView(v1.getStartOffset(), Position.Bias.Forward, p1, b1, r1);
            Rectangle rr1 = (r1s instanceof Rectangle) ? (Rectangle) r1s : r1s.getBounds();
            rr.add(rr1);
        }
        return rr;
    }

    /** Que posicion hay en ese punto: la que diga el hijo que lo contiene. */
    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        Rectangle alloc = getInsideAllocation(a);
        if (isBefore((int) x, (int) y, alloc)) {
            // Antes del principio: la primera posicion.
            int retValue = -1;
            try {
                View v = getViewAtPoint((int) x, (int) y, alloc);
                if (v != null) {
                    retValue = v.getStartOffset();
                }
            } catch (Exception e) {
                retValue = -1;
            }
            if (retValue == -1) {
                retValue = getStartOffset();
                bias[0] = Position.Bias.Forward;
            }
            return retValue;
        } else if (isAfter((int) x, (int) y, alloc)) {
            int retValue = -1;
            try {
                View v = getViewAtPoint((int) x, (int) y, alloc);
                if (v != null) {
                    retValue = v.getEndOffset() - 1;
                }
            } catch (Exception e) {
                retValue = -1;
            }
            if (retValue == -1) {
                retValue = Math.max(0, getEndOffset() - 1);
                bias[0] = Position.Bias.Forward;
            }
            return retValue;
        } else {
            View v = getViewAtPoint((int) x, (int) y, alloc);
            if (v != null) {
                return v.viewToModel(x, y, alloc, bias);
            }
        }
        return -1;
    }

    /** Ver la nota de la clase sobre por que se parte en dos. */
    public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a, int direction,
            Position.Bias[] biasRet) throws BadLocationException {
        if (pos < -1) {
            throw new BadLocationException("invalid position", pos);
        }
        Rectangle alloc = getInsideAllocation(a);

        if (direction == NORTH || direction == SOUTH) {
            return getNextNorthSouthVisualPositionFrom(pos, b, a, direction, biasRet);
        }
        if (direction == EAST || direction == WEST) {
            return getNextEastWestVisualPositionFrom(pos, b, a, direction, biasRet);
        }
        throw new IllegalArgumentException("Bad direction: " + direction);
    }

    /** El hijo que cubre esa posicion, o {@code -1}. */
    public int getViewIndex(int pos, Position.Bias b) {
        if (b == Position.Bias.Backward) {
            pos = pos - 1;
        }
        if ((pos >= getStartOffset()) && (pos < getEndOffset())) {
            return getViewIndexAtPosition(pos);
        }
        return -1;
    }

    /** Si ese punto esta antes del principio de la region; lo contesta cada subclase. */
    protected abstract boolean isBefore(int x, int y, Rectangle alloc);

    protected abstract boolean isAfter(int x, int y, Rectangle alloc);

    protected abstract View getViewAtPoint(int x, int y, Rectangle alloc);

    /** Deja en el rectangulo el lugar de ese hijo; lo contesta cada subclase. */
    protected abstract void childAllocation(int index, Rectangle a);

    /** El hijo que cubre esa posicion, con su lugar puesto en el rectangulo. */
    protected View getViewAtPosition(int pos, Rectangle a) {
        int index = getViewIndexAtPosition(pos);
        if ((index >= 0) && (index < getViewCount())) {
            View v = getView(index);
            if (a != null) {
                childAllocation(index, a);
            }
            return v;
        }
        return null;
    }

    /**
     * Que hijo corresponde a esa posicion.
     *
     * <p>Lo contesta el <em>elemento</em>, no los hijos. Parece un rodeo teniendo los hijos a mano,
     * pero es lo que hace que la respuesta sea la misma antes y despues de armarlos: una vista que
     * todavia no creo a sus hijos, o que los descarto, sigue sabiendo donde caeria cada posicion.
     * Buscando entre los hijos, una vista a medio armar contestaria que la posicion no existe.
     *
     * <p>Quien de verdad tenga hijos que no siguen a los elementos uno a uno lo sobrescribe; es lo
     * que hace {@link ZoneView}.
     */
    protected int getViewIndexAtPosition(int pos) {
        Element elem = getElement();
        return elem.getElementIndex(pos);
    }

    /** El rectangulo de adentro: el lugar menos los margenes. */
    protected Rectangle getInsideAllocation(Shape a) {
        if (a != null) {
            Rectangle alloc;
            if (a instanceof Rectangle) {
                alloc = (Rectangle) a;
            } else {
                alloc = a.getBounds();
            }
            childAlloc.setBounds(alloc);
            childAlloc.x = childAlloc.x + getLeftInset();
            childAlloc.y = childAlloc.y + getTopInset();
            childAlloc.width = childAlloc.width - getLeftInset() - getRightInset();
            childAlloc.height = childAlloc.height - getTopInset() - getBottomInset();
            return childAlloc;
        }
        return null;
    }

    /** Toma los margenes de los atributos de parrafo: sangrias y espacio antes y despues. */
    protected void setParagraphInsets(AttributeSet attr) {
        top = (short) StyleConstants.getSpaceAbove(attr);
        left = (short) StyleConstants.getLeftIndent(attr);
        bottom = (short) StyleConstants.getSpaceBelow(attr);
        right = (short) StyleConstants.getRightIndent(attr);
    }

    protected void setInsets(short top, short left, short bottom, short right) {
        this.top = top;
        this.left = left;
        this.right = right;
        this.bottom = bottom;
    }

    protected short getLeftInset() {
        return left;
    }

    protected short getRightInset() {
        return right;
    }

    protected short getTopInset() {
        return top;
    }

    protected short getBottomInset() {
        return bottom;
    }

    /**
     * Subir o bajar una linea.
     *
     * <p>Por omision no se mueve: una vista que no apila hijos en vertical no sabe que es "la
     * linea de arriba". {@code BoxView} vertical y {@code ParagraphView} lo redefinen.
     */
    protected int getNextNorthSouthVisualPositionFrom(int pos, Position.Bias b, Shape a,
            int direction, Position.Bias[] biasRet) throws BadLocationException {
        return siguienteEnHijos(pos, b, a, direction, biasRet);
    }

    /**
     * Un caracter a la izquierda o a la derecha.
     *
     * <p>Se lo pide al hijo que tiene la posicion; si el hijo dice que se acabo, pasa al de al
     * lado. Es lo que hace que la flecha derecha salte de una palabra a la siguiente sin que
     * nadie tenga que saber donde termina cada una.
     */
    protected int getNextEastWestVisualPositionFrom(int pos, Position.Bias b, Shape a,
            int direction, Position.Bias[] biasRet) throws BadLocationException {
        return siguienteEnHijos(pos, b, a, direction, biasRet);
    }

    /**
     * Le pide la proxima posicion al hijo que tiene la actual, y si se le acaba, al de al lado.
     *
     * <p>El {@code -1} como posicion significa "vengo de afuera": el hijo empieza por su punta.
     * Es como se pasa de un hijo al siguiente sin que ninguno tenga que saber del otro.
     */
    private int siguienteEnHijos(int pos, Position.Bias b, Shape a, int direction,
            Position.Bias[] biasRet) throws BadLocationException {
        if (getViewCount() == 0) {
            return pos;
        }
        boolean haciaAtras = (direction == NORTH || direction == WEST);
        int retValue;
        if (pos == -1) {
            int childIndex = haciaAtras ? getViewCount() - 1 : 0;
            View child = getView(childIndex);
            Shape childBounds = getChildAllocation(childIndex, a);
            retValue = child.getNextVisualPositionFrom(pos, b, childBounds, direction, biasRet);
        } else {
            int increment = haciaAtras ? -1 : 1;
            int childIndex;
            if (b == Position.Bias.Backward && pos > 0) {
                childIndex = getViewIndex(pos - 1, Position.Bias.Forward);
            } else {
                childIndex = getViewIndex(pos, Position.Bias.Forward);
            }
            if (childIndex < 0) {
                return pos;
            }
            View child = getView(childIndex);
            Shape childBounds = getChildAllocation(childIndex, a);
            retValue = child.getNextVisualPositionFrom(pos, b, childBounds, direction, biasRet);
            if ((direction == EAST || direction == WEST) && flipEastAndWestAtEnds(pos, b)) {
                increment = increment * -1;
            }
            childIndex = childIndex + increment;
            if (retValue == -1 && childIndex >= 0 && childIndex < getViewCount()) {
                child = getView(childIndex);
                childBounds = getChildAllocation(childIndex, a);
                retValue = child.getNextVisualPositionFrom(-1, b, childBounds, direction,
                        biasRet);
            }
        }
        return retValue;
    }

    /**
     * Si en los extremos hay que dar vuelta izquierda y derecha.
     *
     * <p>Hace falta en texto que se lee de derecha a izquierda: ahi "la siguiente a la derecha"
     * es la anterior del documento. Sin analisis bidireccional, siempre {@code false}.
     */
    protected boolean flipEastAndWestAtEnds(int position, Position.Bias bias) {
        return false;
    }
}
