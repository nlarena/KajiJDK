package javax.swing.text;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Vector;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;

/**
 * Una vista que reparte su contenido en filas: la base de un parrafo con corte de linea.
 *
 * <h2>Dos arboles de vistas para lo mismo</h2>
 *
 * <p>Hay un arbol <em>logico</em> —una vista por elemento del documento, en
 * {@link #layoutPool}— y otro <em>fisico</em>, que son las filas que se ven. El logico no cambia
 * cuando cambia el ancho; el fisico se rehace entero. Tener los dos es lo que permite volver a
 * cortar en lineas sin volver a crear una vista por tramo de texto.
 *
 * <p>Quien decide donde cortar es la {@link FlowStrategy}, que esta afuera de la vista y se puede
 * cambiar: cortar por palabras, por caracteres o de otra forma es cambiar ese objeto.
 */
public abstract class FlowView extends BoxView {

    /** El ancho disponible para cada fila. */
    protected int layoutSpan;

    /** El arbol logico; ver la nota de la clase. */
    protected View layoutPool;

    protected FlowStrategy strategy;

    public FlowView(Element elem, int axis) {
        super(elem, axis);
        layoutSpan = Integer.MAX_VALUE;
        strategy = new FlowStrategy();
    }

    /** El eje sobre el que fluye el contenido: el perpendicular al de apilado. */
    public int getFlowAxis() {
        if (getAxis() == Y_AXIS) {
            return X_AXIS;
        }
        return Y_AXIS;
    }

    /** Cuanto espacio tiene esa fila; todas lo mismo, salvo que una subclase diga otra cosa. */
    public int getFlowSpan(int index) {
        return layoutSpan;
    }

    /** Donde empieza esa fila. */
    public int getFlowStart(int index) {
        return 0;
    }

    /** Una fila vacia; la subclase decide de que tipo. */
    protected abstract View createRow();

    /**
     * Arma el arbol logico y deja el fisico vacio.
     *
     * <p>Las filas se crean recien al maquetar, cuando se sabe el ancho: antes no se puede saber
     * cuantas hacen falta.
     */
    protected void loadChildren(ViewFactory f) {
        if (layoutPool == null) {
            layoutPool = new LogicalView(getElement());
        }
        layoutPool.setParent(this);
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        strategy.insertUpdate(this, null, null);
    }

    protected int getViewIndexAtPosition(int pos) {
        if (pos >= getStartOffset() && (pos < getEndOffset())) {
            for (int counter = 0; counter < getViewCount(); counter++) {
                View v = getView(counter);
                if (pos >= v.getStartOffset() && pos < v.getEndOffset()) {
                    return counter;
                }
            }
        }
        return -1;
    }

    /** Antes de acomodar, vuelve a cortar en filas si cambio el ancho. */
    protected void layout(int width, int height) {
        final int faxis = getFlowAxis();
        int newSpan;
        if (faxis == X_AXIS) {
            newSpan = width;
        } else {
            newSpan = height;
        }
        if (layoutSpan != newSpan) {
            layoutChanged(faxis);
            layoutChanged(getAxis());
            layoutSpan = newSpan;
        }

        if (!isLayoutValid(faxis)) {
            int heightAxis = getAxis();
            int oldFlowHeight = (heightAxis == X_AXIS) ? getWidth() : getHeight();
            strategy.layout(this);
            int newFlowHeight = (int) getPreferredSpan(heightAxis);
            if (oldFlowHeight != newFlowHeight) {
                View p = getParent();
                if (p != null) {
                    p.preferenceChanged(this, (heightAxis == X_AXIS), (heightAxis == Y_AXIS));
                }
            }
        }
        super.layout(width, height);
    }

    /** El minimo sobre el eje menor es el de la fila mas exigente. */
    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
        if (r == null) {
            r = new SizeRequirements();
        }
        float pref = layoutPool.getPreferredSpan(axis);
        float min = layoutPool.getMinimumSpan(axis);
        r.minimum = (int) min;
        r.preferred = Math.max(r.minimum, (int) pref);
        r.maximum = Integer.MAX_VALUE;
        r.alignment = 0.5f;
        return r;
    }

    public void insertUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        layoutPool.insertUpdate(changes, a, f);
        strategy.insertUpdate(this, changes, getInsideAllocation(a));
    }

    public void removeUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        layoutPool.removeUpdate(changes, a, f);
        strategy.removeUpdate(this, changes, getInsideAllocation(a));
    }

    public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        layoutPool.changedUpdate(changes, a, f);
        strategy.changedUpdate(this, changes, getInsideAllocation(a));
    }

    public void setParent(View parent) {
        super.setParent(parent);
        if (parent == null && layoutPool != null) {
            layoutPool.setParent(null);
        }
    }

    /**
     * Como se reparte el contenido en filas.
     *
     * <p>Esta afuera de la vista para poder cambiarla; ver la nota de {@link FlowView}. La de aca
     * corta por palabras usando los pesos de corte de las vistas.
     */
    public static class FlowStrategy {

        Position damageStart = null;
        Vector<View> viewBuffer;

        public FlowStrategy() {
        }

        void addDamage(FlowView fv, int offset) {
            if (offset >= fv.getStartOffset() && offset < fv.getEndOffset()) {
                if (damageStart == null || offset < damageStart.getOffset()) {
                    try {
                        damageStart = fv.getDocument().createPosition(offset);
                    } catch (BadLocationException e) {
                        damageStart = null;
                    }
                }
            }
        }

        void unsetDamage() {
            damageStart = null;
        }

        public void insertUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
            if (alloc != null) {
                fv.layoutChanged(fv.getFlowAxis());
                java.awt.Container host = fv.getContainer();
                if (host != null) {
                    host.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
                }
            } else {
                fv.layoutChanged(fv.getAxis());
                fv.layoutChanged(fv.getFlowAxis());
            }
        }

        public void removeUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
            insertUpdate(fv, e, alloc);
        }

        public void changedUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
            insertUpdate(fv, e, alloc);
        }

        /** El arbol logico de esa vista. */
        protected View getLogicalView(FlowView fv) {
            return fv.layoutPool;
        }

        /**
         * Rehace las filas.
         *
         * <p>Va creando filas y llenandolas hasta que el contenido se acaba. Cada fila se llena
         * con {@link #layoutRow}, que es quien decide donde cortar.
         */
        public void layout(FlowView fv) {
            View pool = getLogicalView(fv);
            int p0 = fv.getStartOffset();
            int p1 = fv.getEndOffset();

            fv.removeAll();
            int rowIndex = 0;
            int p = p0;
            while (p < p1) {
                View row = fv.createRow();
                fv.append(row);
                int next = layoutRow(fv, rowIndex, p);
                if (next <= p) {
                    // No entro nada: se fuerza el avance para no colgarse.
                    next = p + 1;
                }
                p = next;
                rowIndex = rowIndex + 1;
            }
        }

        /**
         * Llena una fila desde esa posicion y devuelve donde quedo.
         *
         * <p>Va agregando vistas mientras entren; la primera que no entra se parte con
         * {@code breakView}, que es donde se decide cortar por una palabra y no por una letra.
         */
        protected int layoutRow(FlowView fv, int rowIndex, int pos) {
            View row = fv.getView(rowIndex);
            float x = fv.getFlowStart(rowIndex);
            float spanLeft = fv.getFlowSpan(rowIndex);
            int end = fv.getEndOffset();
            int flowAxis = fv.getFlowAxis();

            int p = pos;
            while (p < end && spanLeft >= 0) {
                View v = createView(fv, p, (int) spanLeft, rowIndex);
                if (v == null) {
                    break;
                }
                float chunk = v.getPreferredSpan(flowAxis);
                if (chunk > spanLeft && row.getViewCount() > 0) {
                    // No entra y ya hay algo en la fila: se corta aca.
                    break;
                }
                row.append(v);
                spanLeft = spanLeft - chunk;
                p = v.getEndOffset();
                if (v.getEndOffset() <= pos) {
                    break;
                }
            }
            return p;
        }

        /** Acomoda la fila una vez llena; sin justificado, no hace nada. */
        protected void adjustRow(FlowView fv, int rowIndex, int desiredSpan, int x) {
        }

        void reparentViews(View pool, int startPos) {
        }

        /**
         * La vista que hay que poner a partir de esa posicion, recortada a lo que entra.
         *
         * <p>Devuelve la del arbol logico entera si entra, y un fragmento si no.
         */
        protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
            View pool = getLogicalView(fv);
            int childIndex = pool.getViewIndex(startOffset, Position.Bias.Forward);
            if (childIndex < 0) {
                return null;
            }
            View v = pool.getView(childIndex);
            if (v == null) {
                return null;
            }
            if (startOffset > v.getStartOffset()) {
                v = v.createFragment(startOffset, v.getEndOffset());
            }
            int flowAxis = fv.getFlowAxis();
            float span = v.getPreferredSpan(flowAxis);
            if (span > spanLeft) {
                View partida = v.breakView(flowAxis, v.getStartOffset(), 0f, spanLeft);
                if (partida != null && partida.getEndOffset() > v.getStartOffset()) {
                    return partida;
                }
            }
            return v;
        }
    }

    /**
     * El arbol logico: una vista por elemento hijo, que no se rehace al cambiar el ancho.
     *
     * <p>Es privada en el JDK y aca tambien: se llega a ella por {@link FlowStrategy#getLogicalView}.
     */
    static class LogicalView extends CompositeView {

        LogicalView(Element elem) {
            super(elem);
        }

        protected int getViewIndexAtPosition(int pos) {
            Element elem = getElement();
            if (elem.isLeaf()) {
                return 0;
            }
            return super.getViewIndexAtPosition(pos);
        }

        protected void loadChildren(ViewFactory f) {
            Element elem = getElement();
            if (elem.isLeaf()) {
                View v = new LabelView(elem);
                append(v);
            } else {
                super.loadChildren(f);
            }
        }

        public float getPreferredSpan(int axis) {
            float maxpref = 0;
            float pref = 0;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                pref = pref + v.getPreferredSpan(axis);
                if (v.getBreakWeight(axis, 0, Integer.MAX_VALUE) >= View.ForcedBreakWeight) {
                    maxpref = Math.max(maxpref, pref);
                    pref = 0;
                }
            }
            maxpref = Math.max(maxpref, pref);
            return maxpref;
        }

        public float getMinimumSpan(int axis) {
            float maxmin = 0;
            float min = 0;
            boolean nowrap = false;
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                if (v.getBreakWeight(axis, 0, Integer.MAX_VALUE) == View.BadBreakWeight) {
                    min = min + v.getPreferredSpan(axis);
                    nowrap = true;
                } else if (nowrap) {
                    maxmin = Math.max(min, maxmin);
                    nowrap = false;
                    min = 0;
                }
            }
            maxmin = Math.max(maxmin, min);
            return maxmin;
        }

        protected void forwardUpdateToView(View v, DocumentEvent e, Shape a, ViewFactory f) {
            View parent = v.getParent();
            v.setParent(this);
            super.forwardUpdateToView(v, e, a, f);
            v.setParent(parent);
        }

        // -- lo que una vista logica no necesita, porque no se dibuja ------------------------

        protected boolean isBefore(int x, int y, Rectangle alloc) {
            return false;
        }

        protected boolean isAfter(int x, int y, Rectangle alloc) {
            return false;
        }

        protected View getViewAtPoint(int x, int y, Rectangle alloc) {
            return null;
        }

        protected void childAllocation(int index, Rectangle a) {
        }

        public void paint(java.awt.Graphics g, Shape allocation) {
        }

        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            return null;
        }

        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            return -1;
        }
    }
}
