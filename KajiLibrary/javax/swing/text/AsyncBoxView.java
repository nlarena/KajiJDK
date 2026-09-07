package javax.swing.text;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$ElementChange;

/**
 * Una caja que maqueta a sus hijos en otro hilo.
 *
 * <h2>Para que no se congele la pantalla</h2>
 *
 * <p>Maquetar un documento grande lleva tiempo. Si eso pasa en el hilo de eventos, la ventana no
 * responde mientras dura. Esta vista pone cada hijo en una cola ({@link LayoutQueue}) y lo maqueta
 * en un hilo aparte; mientras tanto, contesta con estimaciones.
 *
 * <h2>El precio: nada es exacto hasta que termina</h2>
 *
 * <p>Como los hijos se van midiendo de a uno, el largo total es una estimacion que se corrige a
 * medida que llegan los resultados. Por eso {@link ChildState} guarda por hijo si su maquetado ya
 * vale, y el {@link ChildLocator} recuerda hasta donde los desplazamientos son de fiar: mas alla de
 * ese punto los calcula suponiendo que los que faltan miden lo que dice la estimacion.
 *
 * <h2>Dos hilos sobre los mismos datos</h2>
 *
 * <p>El hilo de maquetado escribe los tamanos y el de eventos los lee. Todo lo que se comparte se
 * toca bajo el candado de la vista o el del hijo, y nunca los dos a la vez en distinto orden: eso
 * es lo que evita un abrazo mortal entre pintar y maquetar.
 */
public class AsyncBoxView extends View {

    /** Quien sabe donde cae cada hijo. */
    protected ChildLocator locator;

    int axis;
    List<ChildState> stats;
    float majorSpan;
    float minorSpan;
    boolean estimatedMajorSpan;
    int majorAxis;
    int minorAxis;
    float topInset;
    float bottomInset;
    float leftInset;
    float rightInset;
    ChildState minRequest;
    ChildState prefRequest;
    boolean majorChanged;
    boolean minorChanged;
    Runnable flushTask;

    /** Una caja asincronica sobre ese eje. */
    public AsyncBoxView(Element elem, int axis) {
        super(elem);
        stats = new ArrayList<ChildState>();
        this.axis = axis;
        locator = new ChildLocator(this);
        flushTask = new FlushTask(this);
        minorSpan = Short.MAX_VALUE;
    }

    /** El eje en el que se apilan los hijos. */
    public int getMajorAxis() {
        return axis;
    }

    /** El otro. */
    public int getMinorAxis() {
        return (axis == X_AXIS) ? Y_AXIS : X_AXIS;
    }

    public float getTopInset() {
        return topInset;
    }

    public void setTopInset(float i) {
        topInset = i;
    }

    public float getBottomInset() {
        return bottomInset;
    }

    public void setBottomInset(float i) {
        bottomInset = i;
    }

    public float getLeftInset() {
        return leftInset;
    }

    public void setLeftInset(float i) {
        leftInset = i;
    }

    public float getRightInset() {
        return rightInset;
    }

    public void setRightInset(float i) {
        rightInset = i;
    }

    /** Cuanto se come el margen en ese eje. */
    protected float getInsetSpan(int axis) {
        float margin = (axis == X_AXIS)
                ? getLeftInset() + getRightInset() : getTopInset() + getBottomInset();
        return margin;
    }

    /** Si el largo total todavia es una estimacion; ver la nota de la clase. */
    protected void setEstimatedMajorSpan(boolean isEstimated) {
        estimatedMajorSpan = isEstimated;
    }

    protected boolean getEstimatedMajorSpan() {
        return estimatedMajorSpan;
    }

    /** El estado del hijo numero tal. */
    protected ChildState getChildState(int index) {
        synchronized (stats) {
            if ((index >= 0) && (index < stats.size())) {
                return stats.get(index);
            }
            return null;
        }
    }

    /** La cola donde se encolan los maquetados. */
    protected LayoutQueue getLayoutQueue() {
        return LayoutQueue.getDefaultQueue();
    }

    protected ChildState createChildState(View v) {
        return new ChildState(this, v);
    }

    /**
     * Un hijo cambio de largo: se corrige el total sin remaquetar todo.
     *
     * <p>Se resta lo que media y se suma lo que mide ahora. Recalcular la suma entera cada vez
     * seria cuadratico en la cantidad de hijos.
     */
    protected synchronized void majorRequirementChange(ChildState cs, float delta) {
        if (!estimatedMajorSpan) {
            majorSpan = majorSpan + delta;
        }
        majorChanged = true;
    }

    /** Un hijo cambio de ancho: puede cambiar el ancho de la caja. */
    protected synchronized void minorRequirementChange(ChildState cs) {
        minorChanged = true;
    }

    /** Avisa al padre de los cambios juntados hasta ahora. */
    protected void flushRequirementChanges() {
        AbstractDocument doc = (AbstractDocument) getDocument();
        try {
            doc.readLock();

            View parent = null;
            boolean horizontal = false;
            boolean vertical = false;

            synchronized (this) {
                if (majorChanged || minorChanged) {
                    parent = getParent();
                    if (parent != null) {
                        if (axis == X_AXIS) {
                            horizontal = majorChanged;
                            vertical = minorChanged;
                        } else {
                            vertical = majorChanged;
                            horizontal = minorChanged;
                        }
                    }
                    majorChanged = false;
                    minorChanged = false;
                }
            }

            if (parent != null) {
                parent.preferenceChanged(this, horizontal, vertical);
                java.awt.Component c = getContainer();
                if (c != null) {
                    c.repaint();
                }
            }
        } finally {
            doc.readUnlock();
        }
    }

    /** Cambia los hijos y encola el maquetado de los nuevos. */
    public void replace(int offset, int length, View[] views) {
        synchronized (stats) {
            for (int i = 0; i < length; i++) {
                ChildState cs = stats.remove(offset);
                float csSpan = cs.getMajorSpan();
                cs.getChildView().setParent(null);
                if (csSpan != 0) {
                    majorRequirementChange(cs, -csSpan);
                }
            }
            if (views != null) {
                for (int i = 0; i < views.length; i++) {
                    ChildState s = createChildState(views[i]);
                    stats.add(offset + i, s);
                    majorRequirementChange(s, s.getMajorSpan());
                }
            }
        }
        if (views != null) {
            LayoutQueue q = getLayoutQueue();
            for (int i = 0; i < views.length; i++) {
                q.addTask(getChildState(offset + i));
            }
            q.addTask(flushTask);
        }
    }

    protected void loadChildren(ViewFactory f) {
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

    protected synchronized int getViewIndexAtPosition(int pos, Position.Bias b) {
        boolean isBackward = (b == Position.Bias.Backward);
        pos = (isBackward) ? Math.max(0, pos - 1) : pos;
        Element elem = getElement();
        return elem.getElementIndex(pos);
    }

    protected void updateLayout(DocumentEvent$ElementChange ec, DocumentEvent e, Shape a) {
        if (ec != null) {
            // Los hijos cambiaron: el maquetado guardado ya no sirve.
            locator.childChanged(null);
        }
    }

    public void setParent(View parent) {
        super.setParent(parent);
        if ((parent != null) && (getViewCount() == 0)) {
            ViewFactory f = getViewFactory();
            loadChildren(f);
        }
    }

    public synchronized void preferenceChanged(View child, boolean width, boolean height) {
        if (child == null) {
            getParent().preferenceChanged(this, width, height);
        } else {
            if (minRequest == child) {
                minRequest = null;
            }
            if (prefRequest == child) {
                prefRequest = null;
            }
            int index = getViewIndex(child.getStartOffset(), Position.Bias.Forward);
            ChildState cs = getChildState(index);
            if (cs != null) {
                cs.preferenceChanged(width, height);
                LayoutQueue q = getLayoutQueue();
                q.addTask(cs);
                q.addTask(flushTask);
            }
        }
    }

    /** Al cambiar el tamano solo cambia el eje menor: el mayor lo deciden los hijos. */
    public void setSize(float width, float height) {
        setSpanOnAxis(X_AXIS, width);
        setSpanOnAxis(Y_AXIS, height);
    }

    float getSpanOnAxis(int axis) {
        if (axis == getMajorAxis()) {
            return majorSpan;
        }
        return minorSpan;
    }

    void setSpanOnAxis(int axis, float span) {
        float margin = getInsetSpan(axis);
        if (axis == getMinorAxis()) {
            float targetSpan = span - margin;
            if (targetSpan != minorSpan) {
                minorSpan = targetSpan;
                // Cambiar el ancho invalida el alto de todos los hijos.
                int n = getViewCount();
                LayoutQueue q = getLayoutQueue();
                for (int i = 0; i < n; i++) {
                    ChildState cs = getChildState(i);
                    cs.childSizeValid = false;
                    q.addTask(cs);
                }
                q.addTask(flushTask);
            }
        } else {
            // El eje mayor no se impone: se acepta lo que midan los hijos.
            if (estimatedMajorSpan) {
                majorSpan = span - margin;
            }
        }
    }

    public void paint(Graphics g, Shape alloc) {
        synchronized (locator) {
            locator.setAllocation(alloc);
            locator.paintChildren(g);
        }
    }

    public float getPreferredSpan(int axis) {
        float margin = getInsetSpan(axis);
        if (axis == this.axis) {
            return majorSpan + margin;
        }
        if (prefRequest != null) {
            View child = prefRequest.getChildView();
            return child.getPreferredSpan(axis) + margin;
        }
        return getMinorSpan() + margin;
    }

    public float getMinimumSpan(int axis) {
        if (axis == this.axis) {
            return getPreferredSpan(axis);
        }
        if (minRequest != null) {
            View child = minRequest.getChildView();
            return child.getMinimumSpan(axis) + getInsetSpan(axis);
        }
        return getInsetSpan(axis);
    }

    public float getMaximumSpan(int axis) {
        if (axis == this.axis) {
            return getPreferredSpan(axis);
        }
        return Integer.MAX_VALUE;
    }

    /** El ancho es el del hijo mas ancho medido hasta ahora. */
    float getMinorSpan() {
        float span = 0;
        int n = getViewCount();
        for (int i = 0; i < n; i++) {
            ChildState cs = getChildState(i);
            span = Math.max(span, cs.getMinorSpan());
        }
        return span;
    }

    public int getViewCount() {
        synchronized (stats) {
            return stats.size();
        }
    }

    public View getView(int n) {
        ChildState cs = getChildState(n);
        if (cs != null) {
            return cs.getChildView();
        }
        return null;
    }

    public Shape getChildAllocation(int index, Shape a) {
        if (a == null) {
            return null;
        }
        synchronized (locator) {
            locator.setAllocation(a);
            return locator.getChildAllocation(index);
        }
    }

    public int getViewIndex(int pos, Position.Bias b) {
        return getViewIndexAtPosition(pos, b);
    }

    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        int index = getViewIndex(pos, b);
        Shape ca = locator.getChildAllocationSync(index, a);
        View cv = getView(index);
        Shape v = cv.modelToView(pos, ca, b);
        return v;
    }

    public int viewToModel(float x, float y, Shape a, Position.Bias[] biasReturn) {
        int index = locator.getViewIndexAtPoint(x, y, a);
        Shape ca = locator.getChildAllocationSync(index, a);
        View v = getView(index);
        return v.viewToModel(x, y, ca, biasReturn);
    }

    public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a, int direction,
            Position.Bias[] biasRet) throws BadLocationException {
        if (pos < -1) {
            throw new BadLocationException("invalid position", pos);
        }
        return Utilities.getNextVisualPositionFrom(this, pos, b, a, direction, biasRet);
    }

    /**
     * Lo que la caja sabe de un hijo: su tamano, si vale y donde empieza.
     *
     * <p>Es tambien la tarea que se encola: {@link #run} es lo que corre el hilo de maquetado.
     *
     * <p>En el JDK es una clase interna; aca es estatica y recibe la caja como primer parametro,
     * que es la misma firma que el JDK genera en el archivo compilado. Ver la nota de
     * {@link TableView.TableRow}.
     */
    public static class ChildState implements Runnable {

        private final AsyncBoxView caja;
        private View child;
        private float majorSpan;
        private float minorSpan;
        private float minorMin;
        private float minorPref;
        private float minorMax;
        private float majorOffset;
        boolean childSizeValid;
        private boolean minorValid;
        private boolean majorValid;

        /** El estado de ese hijo, todavia sin medir. */
        public ChildState(AsyncBoxView caja, View v) {
            this.caja = caja;
            child = v;
            minorValid = false;
            majorValid = false;
            childSizeValid = false;
            child.setParent(caja);
        }

        public View getChildView() {
            return child;
        }

        /**
         * Mide el hijo. La corre el hilo de maquetado.
         *
         * <p>Toma el candado de lectura del documento: sin eso, el documento podria cambiar en la
         * mitad de la medicion y el resultado no valdria para ningun estado del documento.
         */
        public void run() {
            AbstractDocument doc = (AbstractDocument) caja.getDocument();
            try {
                doc.readLock();
                if (minorValid && majorValid && childSizeValid) {
                    return;
                }
                if (child.getParent() == caja) {
                    // Puede haber sido sacado mientras esperaba en la cola.
                    updateChild();
                    while (!(minorValid && majorValid && childSizeValid)
                            && child.getParent() == caja) {
                        updateChild();
                    }
                }
            } finally {
                doc.readUnlock();
            }
        }

        void updateChild() {
            boolean minorUpdated = false;
            synchronized (this) {
                if (!minorValid) {
                    int minorAxis = caja.getMinorAxis();
                    minorMin = child.getMinimumSpan(minorAxis);
                    minorPref = child.getPreferredSpan(minorAxis);
                    minorMax = child.getMaximumSpan(minorAxis);
                    minorValid = true;
                    minorUpdated = true;
                }
            }
            if (minorUpdated) {
                caja.minorRequirementChange(this);
            }

            boolean majorUpdated = false;
            float delta = 0.0f;
            synchronized (this) {
                if (!majorValid) {
                    float oldSpan = majorSpan;
                    majorSpan = child.getPreferredSpan(caja.axis);
                    delta = majorSpan - oldSpan;
                    majorValid = true;
                    majorUpdated = true;
                }
            }
            if (majorUpdated) {
                caja.majorRequirementChange(this, delta);
                caja.locator.childChanged(this);
            }

            synchronized (this) {
                if (!childSizeValid) {
                    float w;
                    float h;
                    if (caja.axis == X_AXIS) {
                        w = majorSpan;
                        h = getMinorSpan();
                    } else {
                        w = getMinorSpan();
                        h = majorSpan;
                    }
                    childSizeValid = true;
                    child.setSize(w, h);
                }
            }
        }

        /** El ancho del hijo, dentro de lo que la caja le da. */
        public float getMinorSpan() {
            if (minorMax < caja.minorSpan) {
                return minorMax;
            }
            return Math.max(minorMin, caja.minorSpan);
        }

        /** Donde arranca el hijo en el eje menor, segun su alineacion. */
        public float getMinorOffset() {
            if (minorMax < caja.minorSpan) {
                float align = child.getAlignment(caja.getMinorAxis());
                return ((caja.minorSpan - minorMax) * align);
            }
            return 0f;
        }

        public float getMajorSpan() {
            return majorSpan;
        }

        public float getMajorOffset() {
            return majorOffset;
        }

        /** Lo pone el {@link ChildLocator} al recorrer los hijos. */
        public void setMajorOffset(float offs) {
            majorOffset = offs;
        }

        /** Marca lo que hay que volver a medir y encola el trabajo. */
        public void preferenceChanged(boolean width, boolean height) {
            if (caja.axis == X_AXIS) {
                if (width) {
                    majorValid = false;
                }
                if (height) {
                    minorValid = false;
                }
            } else {
                if (width) {
                    minorValid = false;
                }
                if (height) {
                    majorValid = false;
                }
            }
            childSizeValid = false;
        }

        public boolean isLayoutValid() {
            return (minorValid && majorValid && childSizeValid);
        }
    }

    /**
     * Sabe donde cae cada hijo dentro de la caja.
     *
     * <p>Guarda hasta que hijo los desplazamientos ya se calcularon. Cuando uno cambia de tamano,
     * los de mas abajo dejan de valer y se recalculan recien cuando alguien los pide: recalcular
     * todos en cada cambio seria cuadratico.
     */
    public static class ChildLocator {

        private final AsyncBoxView caja;

        /** El ultimo hijo cuyo desplazamiento vale. */
        protected ChildState lastValidOffset;

        /** El lugar que se le dio a la caja la ultima vez que se pinto. */
        protected Rectangle lastAlloc;

        /** Un rectangulo que se reusa para no reservar uno por hijo. */
        protected Rectangle childAlloc;

        /** Un ubicador para esa caja. */
        public ChildLocator(AsyncBoxView caja) {
            this.caja = caja;
            lastAlloc = new Rectangle();
            childAlloc = new Rectangle();
        }

        /** Un hijo cambio: desde el, los desplazamientos dejan de valer. */
        public synchronized void childChanged(ChildState cs) {
            if (lastValidOffset == null) {
                return;
            }
            if (cs == null || cs.getChildView().getStartOffset()
                    < lastValidOffset.getChildView().getStartOffset()) {
                lastValidOffset = cs;
            }
        }

        /** Dibuja los hijos que caen dentro del recorte. */
        public synchronized void paintChildren(Graphics g) {
            Rectangle clip = g.getClipBounds();
            float targetOffset = (caja.axis == X_AXIS)
                    ? clip.x - lastAlloc.x : clip.y - lastAlloc.y;
            int index = getViewIndexAtVisualOffset(targetOffset);
            int n = caja.getViewCount();
            float offs = caja.getChildState(index).getMajorOffset();
            for (int i = index; i < n; i++) {
                ChildState cs = caja.getChildState(i);
                cs.setMajorOffset(offs);
                Shape ca = getChildAllocation(i);
                if (intersectsClip(ca, clip)) {
                    synchronized (cs) {
                        View v = cs.getChildView();
                        v.paint(g, ca);
                    }
                } else {
                    // Ya se paso del recorte: lo que viene tampoco se ve.
                    break;
                }
                offs = offs + cs.getMajorSpan();
            }
        }

        private boolean intersectsClip(Shape ca, Rectangle clip) {
            if (ca == null) {
                return false;
            }
            Rectangle r = ca.getBounds();
            return r.intersects(clip);
        }

        /** El lugar del hijo numero tal, dentro de ese lugar. */
        public synchronized Shape getChildAllocation(int index, Shape a) {
            if (a == null) {
                return null;
            }
            setAllocation(a);
            ChildState cs = caja.getChildState(index);
            if (cs.getChildView().getParent() != caja) {
                return null;
            }
            updateChildOffsetsToIndex(index);
            Shape ca = getChildAllocation(index);
            return ca;
        }

        Shape getChildAllocationSync(int index, Shape a) {
            synchronized (this) {
                return getChildAllocation(index, a);
            }
        }

        /** Que hijo cae en ese punto. */
        public int getViewIndexAtPoint(float x, float y, Shape a) {
            setAllocation(a);
            float targetOffset = (caja.axis == X_AXIS) ? x - lastAlloc.x : y - lastAlloc.y;
            int index = getViewIndexAtVisualOffset(targetOffset);
            return index;
        }

        /** El lugar del hijo numero tal, en el ultimo lugar dado a la caja. */
        protected Shape getChildAllocation(int index) {
            ChildState cs = caja.getChildState(index);
            if (!cs.isLayoutValid()) {
                cs.run();
            }
            if (caja.axis == X_AXIS) {
                childAlloc.x = lastAlloc.x + (int) cs.getMajorOffset();
                childAlloc.y = lastAlloc.y + (int) cs.getMinorOffset();
                childAlloc.width = (int) cs.getMajorSpan();
                childAlloc.height = (int) cs.getMinorSpan();
            } else {
                childAlloc.x = lastAlloc.x + (int) cs.getMinorOffset();
                childAlloc.y = lastAlloc.y + (int) cs.getMajorOffset();
                childAlloc.width = (int) cs.getMinorSpan();
                childAlloc.height = (int) cs.getMajorSpan();
            }
            return childAlloc;
        }

        /** Recuerda el lugar dado a la caja, ya sin margenes. */
        protected void setAllocation(Shape a) {
            if (a instanceof Rectangle) {
                lastAlloc.setBounds((Rectangle) a);
            } else {
                lastAlloc.setBounds(a.getBounds());
            }
            caja.setSize(lastAlloc.width, lastAlloc.height);
            lastAlloc.x = lastAlloc.x + (int) caja.getLeftInset();
            lastAlloc.y = lastAlloc.y + (int) caja.getTopInset();
            lastAlloc.width = lastAlloc.width
                    - (int) (caja.getLeftInset() + caja.getRightInset());
            lastAlloc.height = lastAlloc.height
                    - (int) (caja.getTopInset() + caja.getBottomInset());
        }

        /** Que hijo empieza a esa distancia del principio. */
        protected int getViewIndexAtVisualOffset(float targetOffset) {
            int n = caja.getViewCount();
            if (n > 0) {
                boolean lastValid = (lastValidOffset != null);
                if (lastValidOffset == null) {
                    lastValidOffset = caja.getChildState(0);
                }
                if (targetOffset > caja.majorSpan) {
                    targetOffset = caja.majorSpan;
                }
                if (targetOffset > lastValidOffset.getMajorOffset()) {
                    return updateChildOffsets(targetOffset);
                }
                float offs = 0f;
                for (int i = 0; i < n; i++) {
                    ChildState cs = caja.getChildState(i);
                    float nextOffs = offs + cs.getMajorSpan();
                    if (targetOffset < nextOffs) {
                        return i;
                    }
                    offs = nextOffs;
                }
            }
            return n - 1;
        }

        /** Sigue calculando desplazamientos hasta llegar a esa distancia. */
        int updateChildOffsets(float targetOffset) {
            int n = caja.getViewCount();
            int targetIndex = n - 1;
            int pos = caja.stats.indexOf(lastValidOffset);
            float start = lastValidOffset.getMajorOffset();
            float lastOffset = start;
            for (int i = pos; i < n; i++) {
                ChildState cs = caja.getChildState(i);
                cs.setMajorOffset(lastOffset);
                lastOffset = lastOffset + cs.getMajorSpan();
                lastValidOffset = cs;
                if (targetOffset < lastOffset) {
                    targetIndex = i;
                    break;
                }
            }
            return targetIndex;
        }

        /** Sigue calculando desplazamientos hasta ese hijo. */
        void updateChildOffsetsToIndex(int index) {
            int pos = (lastValidOffset != null) ? caja.stats.indexOf(lastValidOffset) : 0;
            if (index <= pos) {
                return;
            }
            float lastOffset = (lastValidOffset != null) ? lastValidOffset.getMajorOffset() : 0f;
            for (int i = pos; i <= index; i++) {
                ChildState cs = caja.getChildState(i);
                cs.setMajorOffset(lastOffset);
                lastOffset = lastOffset + cs.getMajorSpan();
                lastValidOffset = cs;
            }
        }
    }

    /** La tarea que avisa los cambios juntados; se encola al final de cada tanda. */
    static class FlushTask implements Runnable {

        private final AsyncBoxView caja;

        FlushTask(AsyncBoxView caja) {
            this.caja = caja;
        }

        public void run() {
            caja.flushRequirementChanges();
        }
    }
}
