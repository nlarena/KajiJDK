package javax.swing.text;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$ElementChange;

/**
 * Como se ve un pedazo del documento: la mitad visual del modelo-vista del texto.
 *
 * <h2>Un arbol paralelo</h2>
 *
 * <p>El documento tiene su arbol de {@link Element}; la vista arma otro arbol encima, con una
 * vista por elemento. Son dos arboles y no uno porque no siempre se corresponden: un parrafo largo
 * se ve como varias lineas, y cada linea es una vista, pero en el documento sigue siendo un solo
 * elemento. Esa libertad es todo el punto de esta clase.
 *
 * <h2>Las dos preguntas</h2>
 *
 * <p>Una vista tiene que saber contestar dos cosas, y son inversas:
 *
 * <ul>
 * <li>{@link #modelToView}: donde cae en la pantalla esta posicion del texto. Es lo que ubica el
 * cursor.
 * <li>{@link #viewToModel}: que posicion del texto hay en este punto de la pantalla. Es lo que
 * hace que un clic lleve el cursor donde uno miraba.
 * </ul>
 *
 * <p>El {@link Position.Bias} que llevan las dos es la respuesta a una ambiguedad real: el punto
 * entre dos caracteres pertenece al de la izquierda o al de la derecha, y en un final de linea o
 * en un cambio de sentido de escritura eso cae en dos lugares distintos de la pantalla.
 *
 * <h2>Como se acomoda a los cambios</h2>
 *
 * <p>Cuando el documento cambia, el aviso baja por el arbol: {@link #insertUpdate} y sus hermanas
 * ven si el cambio les toco la estructura ({@link #updateChildren}) y despues lo reenvian a los
 * hijos que abarcan el tramo cambiado ({@link #forwardUpdate}). Una vista que no cambio de forma
 * no hace nada, y por eso escribir una letra no rehace el documento entero.
 *
 * <h2>Los pesos</h2>
 *
 * <p>{@link #getBreakWeight} dice que tan bien se puede cortar una vista en un punto —un espacio
 * corta mejor que el medio de una palabra— y {@link #getResizeWeight} que tan dispuesta esta a
 * estirarse. Los dos son numeros y no si/no porque quien acomoda tiene que elegir el mejor de
 * varios males.
 */
public abstract class View implements SwingConstants {

    /** No se puede cortar aca. */
    public static final int BadBreakWeight = 0;

    /** Se puede cortar, pero mal. */
    public static final int GoodBreakWeight = 1000;

    /** Es un buen lugar para cortar. */
    public static final int ExcellentBreakWeight = 2000;

    /** Hay que cortar aca si o si; es lo que devuelve un fin de linea. */
    public static final int ForcedBreakWeight = 3000;

    public static final int X_AXIS = HORIZONTAL;

    public static final int Y_AXIS = VERTICAL;

    /** Un arreglo de un elemento para devolver el sentido sin reservar memoria. */
    static final Position.Bias[] sharedBiasReturn = new Position.Bias[1];

    private View parent;
    private Element elem;

    /** El primer y el ultimo hijo que toco el ultimo cambio; los usa {@link #forwardUpdate}. */
    int firstUpdateIndex;
    int lastUpdateIndex;

    /** Una vista de ese elemento, todavia sin padre. */
    public View(Element elem) {
        this.elem = elem;
    }

    public View getParent() {
        return parent;
    }

    /** Si se muestra; una vista sin padre se considera visible. */
    public boolean isVisible() {
        return true;
    }

    /** Cuanto querria medir sobre ese eje, en pixeles. */
    public abstract float getPreferredSpan(int axis);

    /** Lo menos que puede medir; por omision, lo preferido. */
    public float getMinimumSpan(int axis) {
        int w = getResizeWeight(axis);
        if (w == 0) {
            return getPreferredSpan(axis);
        }
        return 0;
    }

    /** Lo mas que puede medir; sin tope si se puede estirar. */
    public float getMaximumSpan(int axis) {
        int w = getResizeWeight(axis);
        if (w == 0) {
            return getPreferredSpan(axis);
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Avisa al padre que un hijo cambio de tamano.
     *
     * <p>Sube por el arbol hasta alguien que decida rehacer el maquetado. Que suba en vez de que
     * el padre pregunte es lo que hace que un cambio local cueste lo que cuesta ese cambio y no
     * lo que cuesta el documento.
     */
    public void preferenceChanged(View child, boolean width, boolean height) {
        View parent = getParent();
        if (parent != null) {
            parent.preferenceChanged(this, width, height);
        }
    }

    /** Donde queda su linea de enganche sobre ese eje, de 0 a 1; al medio por omision. */
    public float getAlignment(int axis) {
        return 0.5f;
    }

    /** Se dibuja en esa forma, que es el lugar que le toco. */
    public abstract void paint(Graphics g, Shape allocation);

    /**
     * Le pone padre, o se lo saca con {@code null}.
     *
     * <p>Sacarselo es lo que desarma el subarbol: cada vista le saca el padre a sus hijos, y asi
     * se sueltan las referencias hacia arriba.
     */
    public void setParent(View parent) {
        if (parent == null) {
            for (int i = 0; i < getViewCount(); i++) {
                if (getView(i).getParent() == this) {
                    getView(i).setParent(null);
                }
            }
        }
        this.parent = parent;
    }

    /** Cuantas vistas hijas tiene; cero si es una hoja. */
    public int getViewCount() {
        return 0;
    }

    public View getView(int n) {
        return null;
    }

    public void removeAll() {
        replace(0, getViewCount(), null);
    }

    public void remove(int i) {
        replace(i, 1, null);
    }

    public void insert(int offs, View v) {
        View[] one = new View[1];
        one[0] = v;
        replace(offs, 0, one);
    }

    public void append(View v) {
        View[] one = new View[1];
        one[0] = v;
        replace(getViewCount(), 0, one);
    }

    /** Cambia un tramo de hijos por otro; una vista sin hijos no hace nada. */
    public void replace(int offset, int length, View[] views) {
    }

    /** Que hijo cubre esa posicion del documento, o {@code -1}. */
    public int getViewIndex(int pos, Position.Bias b) {
        return -1;
    }

    /** Que parte de su lugar le toca a ese hijo. */
    public Shape getChildAllocation(int index, Shape a) {
        return null;
    }

    /**
     * A donde va el cursor desde esa posicion en esa direccion.
     *
     * <p>Es la que hace que las flechas del teclado se muevan por lo que se ve y no por como esta
     * guardado: bajar una linea es un salto distinto en cada linea.
     */
    public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a, int direction,
            Position.Bias[] biasRet) throws BadLocationException {
        biasRet[0] = Position.Bias.Forward;
        if (direction == NORTH || direction == SOUTH) {
            return pos;
        }
        if (direction == WEST) {
            return Math.max(getStartOffset(), pos - 1);
        }
        if (direction == EAST) {
            return Math.min(getEndOffset() - 1, pos + 1);
        }
        throw new IllegalArgumentException("Bad direction: " + direction);
    }

    /** Donde cae esa posicion del documento; ver la nota de la clase. */
    public abstract Shape modelToView(int pos, Shape a, Position.Bias b)
            throws BadLocationException;

    /**
     * La region que ocupan las dos posiciones juntas.
     *
     * <p>La union de las dos, y por eso una seleccion que cruza lineas da un rectangulo que las
     * cubre a las dos: quien pinte la seleccion tiene que recorrer las lineas, no confiar en este
     * rectangulo.
     */
    public Shape modelToView(int p0, Position.Bias b0, int p1, Position.Bias b1, Shape a)
            throws BadLocationException {
        Shape s0 = modelToView(p0, a, b0);
        Shape s1;
        if (p1 == getEndOffset()) {
            try {
                s1 = modelToView(p1, a, b1);
            } catch (BadLocationException ble) {
                s1 = null;
            }
            if (s1 == null) {
                Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
                s1 = new Rectangle(alloc.x + alloc.width - 1, alloc.y, 1, alloc.height);
            }
        } else {
            s1 = modelToView(p1, a, b1);
        }
        Rectangle r0 = (s0 instanceof Rectangle) ? (Rectangle) s0 : s0.getBounds();
        Rectangle r1 = (s1 instanceof Rectangle) ? (Rectangle) s1 : s1.getBounds();
        if (r0.y != r1.y) {
            // Dos lineas distintas: el rectangulo cubre de una a la otra, ancho completo.
            Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
            r0.x = alloc.x;
            r0.width = alloc.width;
        }
        r0.add(r1);
        return r0;
    }

    /** Que posicion del documento hay en ese punto; ver la nota de la clase. */
    public abstract int viewToModel(float x, float y, Shape a, Position.Bias[] biasReturn);

    /**
     * Se inserto texto.
     *
     * <p>Los tres avisos hacen lo mismo con distinto nombre: ver si cambio la estructura y
     * reenviar a los hijos que toca.
     */
    public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        if (getViewCount() > 0) {
            Element elem = getElement();
            DocumentEvent$ElementChange ec = e.getChange(elem);
            if (ec != null) {
                if (!updateChildren(ec, e, f)) {
                    ec = null;
                }
            }
            forwardUpdate(ec, e, a, f);
            updateLayout(ec, e, a);
        }
    }

    public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        if (getViewCount() > 0) {
            Element elem = getElement();
            DocumentEvent$ElementChange ec = e.getChange(elem);
            if (ec != null) {
                if (!updateChildren(ec, e, f)) {
                    ec = null;
                }
            }
            forwardUpdate(ec, e, a, f);
            updateLayout(ec, e, a);
        }
    }

    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        if (getViewCount() > 0) {
            Element elem = getElement();
            DocumentEvent$ElementChange ec = e.getChange(elem);
            if (ec != null) {
                if (!updateChildren(ec, e, f)) {
                    ec = null;
                }
            }
            forwardUpdate(ec, e, a, f);
            updateLayout(ec, e, a);
        }
    }

    public Document getDocument() {
        return elem.getDocument();
    }

    public int getStartOffset() {
        return elem.getStartOffset();
    }

    public int getEndOffset() {
        return elem.getEndOffset();
    }

    public Element getElement() {
        return elem;
    }

    /** El contexto donde dibujar, del componente; {@code null} sin componente. */
    public Graphics getGraphics() {
        Container c = getContainer();
        if (c != null) {
            return c.getGraphics();
        }
        return null;
    }

    public AttributeSet getAttributes() {
        return elem.getAttributes();
    }

    /**
     * Se parte en ese punto para entrar en ese espacio.
     *
     * <p>Devuelve {@code this} si no sabe partirse, que es lo que hace una vista atomica. Quien la
     * llama tiene que estar preparado para eso: no siempre se puede cortar.
     */
    public View breakView(int axis, int offset, float pos, float len) {
        return this;
    }

    /** Una vista de un pedazo de este elemento; {@code this} si no sabe hacerlo. */
    public View createFragment(int p0, int p1) {
        return this;
    }

    /** Que tan bien se corta en ese punto; ver la nota de la clase. */
    public int getBreakWeight(int axis, float pos, float len) {
        if (len > getPreferredSpan(axis)) {
            return GoodBreakWeight;
        }
        return BadBreakWeight;
    }

    /** Que tan dispuesta esta a cambiar de tamano; cero es "de ninguna manera". */
    public int getResizeWeight(int axis) {
        return 0;
    }

    /**
     * Le dan ese tamano.
     *
     * <p>Por omision no hace nada: una vista que se acomoda sola lo redefine. Recibir un tamano
     * no es lo mismo que pedirlo, y una vista puede ignorarlo.
     */
    public void setSize(float width, float height) {
    }

    /** El componente donde se dibuja; sube por el arbol hasta encontrarlo. */
    public Container getContainer() {
        View v = getParent();
        return (v != null) ? v.getContainer() : null;
    }

    /** Quien fabrica las vistas hijas; sube por el arbol hasta encontrarlo. */
    public ViewFactory getViewFactory() {
        View v = getParent();
        return (v != null) ? v.getViewFactory() : null;
    }

    /** El texto de ayuda en ese punto; el del hijo que lo contenga. */
    public String getToolTipText(float x, float y, Shape allocation) {
        int viewIndex = getViewIndex(x, y, allocation);
        if (viewIndex >= 0) {
            allocation = getChildAllocation(viewIndex, allocation);
            Rectangle rect = (allocation instanceof Rectangle) ? (Rectangle) allocation
                    : allocation.getBounds();
            if (rect.contains(x, y)) {
                return getView(viewIndex).getToolTipText(x, y, allocation);
            }
        }
        return null;
    }

    /** Que hijo esta en ese punto, o {@code -1}. */
    public int getViewIndex(float x, float y, Shape allocation) {
        return -1;
    }

    /**
     * Rehace los hijos que el cambio de estructura afecto.
     *
     * <p>Devuelve si de verdad cambio algo. Es lo que evita reenviar un aviso a hijos que ya no
     * existen.
     */
    protected boolean updateChildren(DocumentEvent$ElementChange ec, DocumentEvent e,
            ViewFactory f) {
        Element[] removedElems = ec.getChildrenRemoved();
        Element[] addedElems = ec.getChildrenAdded();
        View[] added = null;
        if (addedElems != null) {
            added = new View[addedElems.length];
            for (int i = 0; i < addedElems.length; i++) {
                added[i] = f.create(addedElems[i]);
            }
        }
        int nremoved = 0;
        int index = ec.getIndex();
        if (removedElems != null) {
            nremoved = removedElems.length;
        }
        replace(index, nremoved, added);
        return true;
    }

    /**
     * Reenvia el aviso a los hijos que abarcan el tramo cambiado.
     *
     * <p>Solo a esos: recorrer todos costaria lo mismo que rehacer el documento.
     */
    protected void forwardUpdate(DocumentEvent$ElementChange ec, DocumentEvent e, Shape a,
            ViewFactory f) {
        calculateUpdateIndexes(e);

        int hole0 = lastUpdateIndex + 1;
        int hole1 = hole0;
        Element[] addedElems = (ec != null) ? ec.getChildrenAdded() : null;
        if ((addedElems != null) && (addedElems.length > 0)) {
            hole0 = ec.getIndex();
            hole1 = hole0 + addedElems.length - 1;
        }

        // Los hijos nuevos ya estan armados: no hace falta avisarles.
        for (int i = firstUpdateIndex; i <= lastUpdateIndex; i++) {
            if (!((i >= hole0) && (i <= hole1))) {
                View v = getView(i);
                if (v != null) {
                    Shape childAlloc = getChildAllocation(i, a);
                    forwardUpdateToView(v, e, childAlloc, f);
                }
            }
        }
        lastUpdateIndex = Math.max(lastUpdateIndex - 1, 0);
        firstUpdateIndex = Math.min(firstUpdateIndex, lastUpdateIndex);
    }

    /** Que hijos toca el cambio; deja el rango en los dos campos. */
    void calculateUpdateIndexes(DocumentEvent e) {
        int pos = e.getOffset();
        firstUpdateIndex = getViewIndex(pos, Position.Bias.Forward);
        if (firstUpdateIndex == -1 && e.getType() == javax.swing.event.DocumentEvent$EventType.REMOVE
                && pos >= getEndOffset()) {
            firstUpdateIndex = getViewCount() - 1;
        }
        lastUpdateIndex = firstUpdateIndex;
        View v = (firstUpdateIndex >= 0) ? getView(firstUpdateIndex) : null;
        if ((v != null) && (v.getEndOffset() == pos)) {
            // Justo en el borde: el cambio toca tambien al hijo de al lado.
            lastUpdateIndex = Math.min(firstUpdateIndex + 1, getViewCount() - 1);
        }
    }

    /** Deja los indices como si el cambio ya se hubiera aplicado. */
    void updateAfterChange() {
    }

    protected void forwardUpdateToView(View v, DocumentEvent e, Shape a, ViewFactory f) {
        DocumentEvent$EventType type = e.getType();
        if (type == DocumentEvent$EventType.INSERT) {
            v.insertUpdate(e, a, f);
        } else if (type == DocumentEvent$EventType.REMOVE) {
            v.removeUpdate(e, a, f);
        } else {
            v.changedUpdate(e, a, f);
        }
    }

    /** Pide que se rehaga el maquetado si el cambio lo amerita. */
    protected void updateLayout(DocumentEvent$ElementChange ec, DocumentEvent e, Shape a) {
        if ((ec != null) && (a != null)) {
            preferenceChanged(null, true, true);
            Container host = getContainer();
            if (host != null) {
                host.repaint();
            }
        }
    }

    /** @deprecated es {@link #modelToView(int, Shape, Position.Bias)} con sentido hacia adelante. */
    @Deprecated
    public Shape modelToView(int pos, Shape a) throws BadLocationException {
        return modelToView(pos, a, Position.Bias.Forward);
    }

    /** @deprecated es {@link #viewToModel(float, float, Shape, Position.Bias[])}. */
    @Deprecated
    public int viewToModel(float x, float y, Shape a) {
        sharedBiasReturn[0] = Position.Bias.Forward;
        return viewToModel(x, y, a, sharedBiasReturn);
    }
}
