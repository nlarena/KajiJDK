package java.awt;

import java.awt.event.AdjustmentListener;
import java.io.Serializable;

/**
 * La barra de desplazamiento de un {@link ScrollPane}.
 *
 * <p>Es un {@link Adjustable} y no un {@link Scrollbar}, y la diferencia importa: el rango no lo
 * decide quien la usa sino el panel, a partir del tamaño del hijo y del suyo. Por eso
 * {@link #setMinimum}, {@link #setMaximum} y {@link #setVisibleAmount} **tiran** en vez de hacer
 * algo. Fingir que aceptan el pedido y después pisarlo en el siguiente ajuste sería peor: el
 * programa creería que mandó él.
 *
 * <p>Lo que sí se puede cambiar es el valor y los dos incrementos, que son decisiones de quien
 * desplaza, no del panel.
 */
public final class ScrollPaneAdjustable implements Adjustable, Serializable {

    private static final long serialVersionUID = -3359745691033257079L;

    /** El panel que la manda. */
    private final ScrollPane sp;

    /** Acostada o parada. */
    private final int orientation;

    /** El valor actual. */
    private int value;

    /** El piso. */
    private int minimum;

    /** El tope. */
    private int maximum;

    /** Cuánto se ve de una. */
    private int visibleAmount;

    /** Cuánto salta con las flechas. */
    private int unitIncrement = 1;

    /** Cuánto salta al apretar el canal. */
    private int blockIncrement = 1;

    /** Si el usuario la tiene agarrada. */
    private transient boolean isAdjusting;

    /** Los oyentes, encadenados. */
    private AdjustmentListener adjustmentListener;

    /** La arma para ese panel, con ese oyente interno y esa orientación. */
    ScrollPaneAdjustable(ScrollPane sp, AdjustmentListener l, int orientation) {
        this.sp = sp;
        this.orientation = orientation;
        this.adjustmentListener = l;
    }

    /**
     * Fija el rango; lo llama el panel cuando cambia de tamaño el hijo o él.
     *
     * <p>El valor se recorta al rango nuevo, porque el hijo puede haber achicado.
     */
    void setSpan(int min, int max, int visible) {
        this.minimum = min;
        this.maximum = Math.max(max, min + 1);
        this.visibleAmount = Math.min(visible, this.maximum - this.minimum);
        this.value = Math.max(this.minimum,
                Math.min(this.value, this.maximum - this.visibleAmount));
    }

    /** Acostada o parada. */
    public int getOrientation() {
        return this.orientation;
    }

    /**
     * No se puede.
     *
     * @throws AWTError siempre: el rango lo fija el panel
     */
    public void setMinimum(int min) {
        throw new AWTError("Can not set the minimum of this scrollbar");
    }

    /** El piso. */
    public int getMinimum() {
        return this.minimum;
    }

    /**
     * No se puede.
     *
     * @throws AWTError siempre: el rango lo fija el panel
     */
    public void setMaximum(int max) {
        throw new AWTError("Can not set the maximum of this scrollbar");
    }

    /** El tope. */
    public int getMaximum() {
        return this.maximum;
    }

    /** Cuánto salta con las flechas; nunca menos de 1. */
    public synchronized void setUnitIncrement(int u) {
        if (u != this.unitIncrement) {
            this.unitIncrement = Math.max(1, u);
        }
    }

    /** Cuánto salta con las flechas. */
    public int getUnitIncrement() {
        return this.unitIncrement;
    }

    /** Cuánto salta al apretar el canal; nunca menos de 1. */
    public synchronized void setBlockIncrement(int b) {
        if (b != this.blockIncrement) {
            this.blockIncrement = Math.max(1, b);
        }
    }

    /** Cuánto salta al apretar el canal. */
    public int getBlockIncrement() {
        return this.blockIncrement;
    }

    /**
     * No se puede.
     *
     * @throws AWTError siempre: lo que se ve de una es el tamaño del panel
     */
    public void setVisibleAmount(int v) {
        throw new AWTError("Can not set the visible amount of this scrollbar");
    }

    /** Cuánto se ve de una. */
    public int getVisibleAmount() {
        return this.visibleAmount;
    }

    /** Dice si el usuario la tiene agarrada. */
    public void setValueIsAdjusting(boolean b) {
        if (this.isAdjusting != b) {
            this.isAdjusting = b;
        }
    }

    /** Si el usuario la tiene agarrada. */
    public boolean getValueIsAdjusting() {
        return this.isAdjusting;
    }

    /**
     * Desplaza a ese valor.
     *
     * <p>Se recorta al rango, y además **mueve el hijo del panel**: la barra y la posición de lo que
     * se ve son la misma cosa vista de dos lados.
     */
    public void setValue(int v) {
        this.setTypedValue(v);
    }

    /** Recorta y mueve. */
    private void setTypedValue(int v) {
        int nuevo = Math.max(this.minimum, Math.min(v, this.maximum - this.visibleAmount));
        if (nuevo == this.value) {
            return;
        }
        this.value = nuevo;
        if (this.sp != null) {
            Point p = this.sp.getScrollPosition();
            if (this.orientation == Adjustable.HORIZONTAL) {
                this.sp.setScrollPosition(nuevo, p.y);
            } else {
                this.sp.setScrollPosition(p.x, nuevo);
            }
        }
    }

    /** El valor actual. */
    public int getValue() {
        return this.value;
    }

    /** Agrega un oyente; `null` no hace nada. */
    public synchronized void addAdjustmentListener(AdjustmentListener l) {
        if (l == null) {
            return;
        }
        this.adjustmentListener = AWTEventMulticaster.add(this.adjustmentListener, l);
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

    public String toString() {
        return this.getClass().getName() + "[" + this.paramString() + "]";
    }

    /** Lo que la distingue de otra barra, para depurar. */
    public String paramString() {
        return (this.orientation == Adjustable.VERTICAL ? "vertical," : "horizontal,")
                + "[0.." + this.maximum + "]" + ",val=" + this.value + ",vis=" + this.visibleAmount
                + ",unit=" + this.unitIncrement + ",block=" + this.blockIncrement
                + ",isAdjusting=" + this.isAdjusting;
    }
}
