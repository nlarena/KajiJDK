package java.awt;

import java.awt.event.AdjustmentListener;
import java.io.Serializable;

/**
 * The scrollbar of a {@link ScrollPane}.
 *
 * <p>It is an {@link Adjustable} and not a {@link Scrollbar}, and the difference matters: the range
 * is not decided by whoever uses it but by the pane, from the size of the child and its own. That
 * is why {@link #setMinimum}, {@link #setMaximum} and {@link #setVisibleAmount} **throw** instead
 * of doing something. Pretending to accept the request and then overwriting it at the next
 * adjustment would be worse: the program would believe it was in charge.
 *
 * <p>What can be changed is the value and the two increments, which are decisions of whoever
 * scrolls, not of the pane.
 */
public final class ScrollPaneAdjustable implements Adjustable, Serializable {

    private static final long serialVersionUID = -3359745691033257079L;

    /** The pane that is in charge of it. */
    private final ScrollPane sp;

    /** Lying down or standing up. */
    private final int orientation;

    /** The current value. */
    private int value;

    /** The floor. */
    private int minimum;

    /** The ceiling. */
    private int maximum;

    /** How much is seen at once. */
    private int visibleAmount;

    /** How much it jumps with the arrows. */
    private int unitIncrement = 1;

    /** How much it jumps when the channel is pressed. */
    private int blockIncrement = 1;

    /** Whether the user is holding it. */
    private transient boolean isAdjusting;

    /** The listeners, chained. */
    private AdjustmentListener adjustmentListener;

    /** Builds it for that pane, with that internal listener and that orientation. */
    ScrollPaneAdjustable(ScrollPane sp, AdjustmentListener l, int orientation) {
        this.sp = sp;
        this.orientation = orientation;
        this.adjustmentListener = l;
    }

    /**
     * Sets the range; the pane calls it when the child or the pane itself changes size.
     *
     * <p>The value is clamped to the new range, because the child may have shrunk.
     */
    void setSpan(int min, int max, int visible) {
        this.minimum = min;
        this.maximum = Math.max(max, min + 1);
        this.visibleAmount = Math.min(visible, this.maximum - this.minimum);
        this.value = Math.max(this.minimum,
                Math.min(this.value, this.maximum - this.visibleAmount));
    }

    /** Lying down or standing up. */
    public int getOrientation() {
        return this.orientation;
    }

    /**
     * Not possible.
     *
     * @throws AWTError always: the range is set by the pane
     */
    public void setMinimum(int min) {
        throw new AWTError("Can not set the minimum of this scrollbar");
    }

    /** The floor. */
    public int getMinimum() {
        return this.minimum;
    }

    /**
     * Not possible.
     *
     * @throws AWTError always: the range is set by the pane
     */
    public void setMaximum(int max) {
        throw new AWTError("Can not set the maximum of this scrollbar");
    }

    /** The ceiling. */
    public int getMaximum() {
        return this.maximum;
    }

    /** How much it jumps with the arrows; never less than 1. */
    public synchronized void setUnitIncrement(int u) {
        if (u != this.unitIncrement) {
            this.unitIncrement = Math.max(1, u);
        }
    }

    /** How much it jumps with the arrows. */
    public int getUnitIncrement() {
        return this.unitIncrement;
    }

    /** How much it jumps when the channel is pressed; never less than 1. */
    public synchronized void setBlockIncrement(int b) {
        if (b != this.blockIncrement) {
            this.blockIncrement = Math.max(1, b);
        }
    }

    /** How much it jumps when the channel is pressed. */
    public int getBlockIncrement() {
        return this.blockIncrement;
    }

    /**
     * Not possible.
     *
     * @throws AWTError always: what is seen at once is the size of the pane
     */
    public void setVisibleAmount(int v) {
        throw new AWTError("Can not set the visible amount of this scrollbar");
    }

    /** How much is seen at once. */
    public int getVisibleAmount() {
        return this.visibleAmount;
    }

    /** Says whether the user is holding it. */
    public void setValueIsAdjusting(boolean b) {
        if (this.isAdjusting != b) {
            this.isAdjusting = b;
        }
    }

    /** Whether the user is holding it. */
    public boolean getValueIsAdjusting() {
        return this.isAdjusting;
    }

    /**
     * Scrolls to that value.
     *
     * <p>It is clamped to the range, and it also **moves the pane's child**: the bar and the
     * position of what is seen are the same thing looked at from two sides.
     */
    public void setValue(int v) {
        this.setTypedValue(v);
    }

    /** Clamps and moves. */
    private void setTypedValue(int v) {
        int newValue = Math.max(this.minimum, Math.min(v, this.maximum - this.visibleAmount));
        if (newValue == this.value) {
            return;
        }
        this.value = newValue;
        if (this.sp != null) {
            Point p = this.sp.getScrollPosition();
            if (this.orientation == Adjustable.HORIZONTAL) {
                this.sp.setScrollPosition(newValue, p.y);
            } else {
                this.sp.setScrollPosition(p.x, newValue);
            }
        }
    }

    /** The current value. */
    public int getValue() {
        return this.value;
    }

    /** Adds a listener; `null` does nothing. */
    public synchronized void addAdjustmentListener(AdjustmentListener l) {
        if (l == null) {
            return;
        }
        this.adjustmentListener = AWTEventMulticaster.add(this.adjustmentListener, l);
    }

    /** Removes a listener. */
    public synchronized void removeAdjustmentListener(AdjustmentListener l) {
        if (l == null) {
            return;
        }
        this.adjustmentListener = AWTEventMulticaster.remove(this.adjustmentListener, l);
    }

    /** The listeners that are set. */
    public synchronized AdjustmentListener[] getAdjustmentListeners() {
        return AWTEventMulticaster.getListeners(this.adjustmentListener,
                AdjustmentListener.class);
    }

    public String toString() {
        return this.getClass().getName() + "[" + this.paramString() + "]";
    }

    /** What tells it apart from another bar, for debugging. */
    public String paramString() {
        return (this.orientation == Adjustable.VERTICAL ? "vertical," : "horizontal,")
                + "[0.." + this.maximum + "]" + ",val=" + this.value + ",vis=" + this.visibleAmount
                + ",unit=" + this.unitIncrement + ",block=" + this.blockIncrement
                + ",isAdjusting=" + this.isAdjusting;
    }
}
