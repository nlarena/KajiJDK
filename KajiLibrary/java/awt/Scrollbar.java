package java.awt;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.EventListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;

/**
 * Una barra de desplazamiento: un valor dentro de un rango, movido con un cursor.
 *
 * <p>El detalle que confunde a todo el mundo es que **el valor máximo no se alcanza nunca**. La
 * barra tiene un ancho visible, y el valor sólo llega hasta {@code maximum - visibleAmount}. Es
 * coherente si se piensa en para qué existe: el valor es el renglón de arriba de lo que se ve, y el
 * renglón de arriba nunca puede ser el último, porque abajo de él tiene que entrar una pantalla.
 *
 * <p>Los dos incrementos son distintos: el de unidad es el de las flechas de las puntas, el de
 * bloque es el de apretar el canal, que salta una pantalla.
 */
public class Scrollbar extends Component implements Adjustable, Accessible {

    private static final long serialVersionUID = 8451667562882310543L;

    private static int scrollbarCounter = 0;

    /** Acostada. */
    public static final int HORIZONTAL = 0;

    /** Parada. */
    public static final int VERTICAL = 1;

    /** El valor actual. */
    int value;

    /** El tope. */
    int maximum;

    /** El piso. */
    int minimum;

    /** Cuánto del rango se ve de una. */
    int visibleAmount;

    /** Acostada o parada. */
    int orientation;

    /** Cuánto salta con las flechas. */
    int lineIncrement = 1;

    /** Cuánto salta al apretar el canal. */
    int pageIncrement = 10;

    /** Si el usuario tiene el cursor agarrado. */
    transient boolean isAdjusting;

    /** Los oyentes, encadenados. */
    transient AdjustmentListener adjustmentListener;

    /** Una barra parada, de 0 a 100, con 10 visibles. */
    public Scrollbar() throws HeadlessException {
        this(VERTICAL, 0, 10, 0, 100);
    }

    /** Una barra con esa orientación, de 0 a 100, con 10 visibles. */
    public Scrollbar(int orientation) throws HeadlessException {
        this(orientation, 0, 10, 0, 100);
    }

    /**
     * Una barra con todo dicho.
     *
     * @throws IllegalArgumentException si la orientación no es {@link #HORIZONTAL} ni
     *     {@link #VERTICAL}
     */
    public Scrollbar(int orientation, int value, int visible, int minimum, int maximum)
            throws HeadlessException {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("illegal scrollbar orientation");
        }
        this.orientation = orientation;
        this.setValues(value, visible, minimum, maximum);
    }

    String constructComponentName() {
        synchronized (Scrollbar.class) {
            String n = "scrollbar" + scrollbarCounter;
            scrollbarCounter = scrollbarCounter + 1;
            return n;
        }
    }

    /** La declara mostrable. */
    public void addNotify() {
        super.addNotify();
    }

    /** Acostada o parada. */
    public int getOrientation() {
        return this.orientation;
    }

    /**
     * La acuesta o la para.
     *
     * @throws IllegalArgumentException si no es una de las dos constantes
     */
    public void setOrientation(int orientation) {
        synchronized (this) {
            if (orientation == this.orientation) {
                return;
            }
            if (orientation != HORIZONTAL && orientation != VERTICAL) {
                throw new IllegalArgumentException("illegal scrollbar orientation");
            }
            this.orientation = orientation;
        }
        this.invalidate();
    }

    /** El valor actual. */
    public int getValue() {
        return this.value;
    }

    /**
     * Cambia el valor.
     *
     * <p>Se recorta al rango válido, que llega hasta {@code maximum - visibleAmount} y no hasta
     * {@code maximum}.
     */
    public void setValue(int newValue) {
        this.setValues(newValue, this.visibleAmount, this.minimum, this.maximum);
    }

    /** El piso. */
    public int getMinimum() {
        return this.minimum;
    }

    /**
     * Cambia el piso.
     *
     * <p>Un piso mayor que el tope lo empuja: el rango no puede quedar dado vuelta.
     */
    public void setMinimum(int newMinimum) {
        this.setValues(this.value, this.visibleAmount, newMinimum, this.maximum);
    }

    /** El tope. */
    public int getMaximum() {
        return this.maximum;
    }

    /** Cambia el tope; uno menor que el piso lo empuja. */
    public void setMaximum(int newMaximum) {
        this.setValues(this.value, this.visibleAmount, this.minimum, newMaximum);
    }

    /** Cuánto del rango se ve de una. */
    public int getVisibleAmount() {
        return this.visibleAmount;
    }

    /**
     * Cuánto se ve de una.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getVisibleAmount}.
     */
    @Deprecated
    public int getVisible() {
        return this.visibleAmount;
    }

    /** Cambia cuánto se ve de una. */
    public void setVisibleAmount(int newAmount) {
        this.setValues(this.value, newAmount, this.minimum, this.maximum);
    }

    /** Cuánto salta con las flechas; nunca menos de 1. */
    public void setUnitIncrement(int v) {
        this.setLineIncrement(v);
    }

    /**
     * Cuánto salta con las flechas.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #setUnitIncrement}.
     */
    @Deprecated
    public synchronized void setLineIncrement(int v) {
        this.lineIncrement = Math.max(1, v);
    }

    /** Cuánto salta con las flechas. */
    public int getUnitIncrement() {
        return this.lineIncrement;
    }

    /**
     * Cuánto salta con las flechas.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getUnitIncrement}.
     */
    @Deprecated
    public int getLineIncrement() {
        return this.lineIncrement;
    }

    /** Cuánto salta al apretar el canal; nunca menos de 1. */
    public void setBlockIncrement(int v) {
        this.setPageIncrement(v);
    }

    /**
     * Cuánto salta al apretar el canal.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #setBlockIncrement}.
     */
    @Deprecated
    public synchronized void setPageIncrement(int v) {
        this.pageIncrement = Math.max(1, v);
    }

    /** Cuánto salta al apretar el canal. */
    public int getBlockIncrement() {
        return this.pageIncrement;
    }

    /**
     * Cuánto salta al apretar el canal.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getBlockIncrement}.
     */
    @Deprecated
    public int getPageIncrement() {
        return this.pageIncrement;
    }

    /**
     * Cambia las cuatro medidas de una.
     *
     * <p>Existe porque cambiarlas de a una pasa por estados imposibles —un valor fuera del rango
     * nuevo, un piso arriba del tope— y cada paso recortaría de más. Acá se ajustan todas juntas y
     * recién después se recorta.
     */
    public void setValues(int value, int visible, int minimum, int maximum) {
        synchronized (this) {
            if (minimum == Integer.MAX_VALUE) {
                minimum = Integer.MAX_VALUE - 1;
            }
            if (maximum <= minimum) {
                maximum = minimum + 1;
            }
            // El ancho visible no puede pasarse del rango, ni ser cero: una barra que no muestra
            // nada no tiene cursor que agarrar.
            long anchoMaximo = (long) maximum - (long) minimum;
            if (anchoMaximo > Integer.MAX_VALUE) {
                anchoMaximo = Integer.MAX_VALUE;
            }
            if (visible > (int) anchoMaximo) {
                visible = (int) anchoMaximo;
            }
            if (visible < 1) {
                visible = 1;
            }
            if (value < minimum) {
                value = minimum;
            }
            if (value > maximum - visible) {
                value = maximum - visible;
            }
            this.value = value;
            this.visibleAmount = visible;
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }

    /** Si el usuario tiene el cursor agarrado. */
    public boolean getValueIsAdjusting() {
        return this.isAdjusting;
    }

    /**
     * Dice si el usuario tiene el cursor agarrado.
     *
     * <p>Sirve para no recalcular en cada píxel del arrastre: el oyente puede esperar a que esto sea
     * `false` y hacer el trabajo caro una sola vez.
     */
    public void setValueIsAdjusting(boolean b) {
        this.isAdjusting = b;
    }

    /** Agrega un oyente; `null` no hace nada. */
    public synchronized void addAdjustmentListener(AdjustmentListener l) {
        if (l == null) {
            return;
        }
        this.adjustmentListener = AWTEventMulticaster.add(this.adjustmentListener, l);
        this.enableEvents(AWTEvent.ADJUSTMENT_EVENT_MASK);
    }

    /** Saca un oyente. */
    public synchronized void removeAdjustmentListener(AdjustmentListener l) {
        if (l == null) {
            return;
        }
        this.adjustmentListener = AWTEventMulticaster.remove(this.adjustmentListener, l);
    }

    /** Los oyentes puestos. */
    public synchronized AdjustmentListener[] getAdjustmentListeners() {
        return AWTEventMulticaster.getListeners(this.adjustmentListener,
                AdjustmentListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == AdjustmentListener.class) {
            return AWTEventMulticaster.getListeners(this.adjustmentListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof AdjustmentEvent) {
            this.processAdjustmentEvent((AdjustmentEvent) e);
            return;
        }
        super.processEvent(e);
    }

    /** Les avisa a los oyentes de ajuste. */
    protected void processAdjustmentEvent(AdjustmentEvent e) {
        AdjustmentListener l = this.adjustmentListener;
        if (l != null) {
            l.adjustmentValueChanged(e);
        }
    }

    protected String paramString() {
        return super.paramString() + ",val=" + this.value + ",vis=" + this.visibleAmount
                + ",min=" + this.minimum + ",max=" + this.maximum
                + (this.orientation == VERTICAL ? ",vert" : ",horz");
    }

    /** La accesibilidad de la barra. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTScrollBar();
        }
        return this.accessibleContext;
    }

    /**
     * La accesibilidad de una barra de desplazamiento.
     *
     * <p>El máximo que informa es {@link Scrollbar#getMaximum}, o sea el **nominal**, aunque la
     * barra nunca lo alcance. Informar el alcanzable sería más útil y sería inventar: el JDK informa
     * el nominal, y se comprobó.
     */
    protected class AccessibleAWTScrollBar extends AccessibleAWTComponent
            implements AccessibleValue {

        /** Para las subclases. */
        protected AccessibleAWTScrollBar() {
        }

        public AccessibleValue getAccessibleValue() {
            return this;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SCROLL_BAR;
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (Scrollbar.this.getValueIsAdjusting()) {
                s.add(AccessibleState.BUSY);
            }
            if (Scrollbar.this.getOrientation() == VERTICAL) {
                s.add(AccessibleState.VERTICAL);
            } else {
                s.add(AccessibleState.HORIZONTAL);
            }
            return s;
        }

        public Number getCurrentAccessibleValue() {
            return Integer.valueOf(Scrollbar.this.getValue());
        }

        /**
         * Cambia el valor.
         *
         * @return `true` si el valor no era `null`
         */
        public boolean setCurrentAccessibleValue(Number n) {
            if (n == null) {
                return false;
            }
            Scrollbar.this.setValue(n.intValue());
            return true;
        }

        public Number getMinimumAccessibleValue() {
            return Integer.valueOf(Scrollbar.this.getMinimum());
        }

        /** El tope nominal. */
        public Number getMaximumAccessibleValue() {
            return Integer.valueOf(Scrollbar.this.getMaximum());
        }
    }
}
