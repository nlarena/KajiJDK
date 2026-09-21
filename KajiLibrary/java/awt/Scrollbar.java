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
 * A scrollbar: a value inside a range, moved with a thumb.
 *
 * <p>The detail that confuses everybody is that **the maximum value is never reached**. The bar has
 * a visible width, and the value only goes up to {@code maximum - visibleAmount}. It is coherent if
 * one thinks about what it is for: the value is the top line of what is seen, and the top line can
 * never be the last one, because a screenful has to fit below it.
 *
 * <p>The two increments are different: the unit one belongs to the arrows at the ends, the block
 * one to pressing the channel, which jumps a screenful.
 */
public class Scrollbar extends Component implements Adjustable, Accessible {

    private static final long serialVersionUID = 8451667562882310543L;

    private static int scrollbarCounter = 0;

    /** Lying down. */
    public static final int HORIZONTAL = 0;

    /** Standing up. */
    public static final int VERTICAL = 1;

    /** The current value. */
    int value;

    /** The ceiling. */
    int maximum;

    /** The floor. */
    int minimum;

    /** How much of the range is seen at once. */
    int visibleAmount;

    /** Lying down or standing up. */
    int orientation;

    /** How much it jumps with the arrows. */
    int lineIncrement = 1;

    /** How much it jumps when the channel is pressed. */
    int pageIncrement = 10;

    /** Whether the user is holding the thumb. */
    transient boolean isAdjusting;

    /** The listeners, chained. */
    transient AdjustmentListener adjustmentListener;

    /** A standing bar, from 0 to 100, with 10 visible. */
    public Scrollbar() throws HeadlessException {
        this(VERTICAL, 0, 10, 0, 100);
    }

    /** A bar with that orientation, from 0 to 100, with 10 visible. */
    public Scrollbar(int orientation) throws HeadlessException {
        this(orientation, 0, 10, 0, 100);
    }

    /**
     * A bar with everything given.
     *
     * @throws IllegalArgumentException if the orientation is neither {@link #HORIZONTAL} nor
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

    /** Declares it showable. */
    public void addNotify() {
        super.addNotify();
    }

    /** Lying down or standing up. */
    public int getOrientation() {
        return this.orientation;
    }

    /**
     * Lays it down or stands it up.
     *
     * @throws IllegalArgumentException if it is not one of the two constants
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

    /** The current value. */
    public int getValue() {
        return this.value;
    }

    /**
     * Changes the value.
     *
     * <p>It is clamped to the valid range, which goes up to {@code maximum - visibleAmount} and not
     * up to {@code maximum}.
     */
    public void setValue(int newValue) {
        this.setValues(newValue, this.visibleAmount, this.minimum, this.maximum);
    }

    /** The floor. */
    public int getMinimum() {
        return this.minimum;
    }

    /**
     * Changes the floor.
     *
     * <p>A floor above the ceiling pushes the ceiling up: the range cannot be left reversed.
     */
    public void setMinimum(int newMinimum) {
        this.setValues(this.value, this.visibleAmount, newMinimum, this.maximum);
    }

    /** The ceiling. */
    public int getMaximum() {
        return this.maximum;
    }

    /** Changes the ceiling; one below the floor is pushed up to just above it. */
    public void setMaximum(int newMaximum) {
        this.setValues(this.value, this.visibleAmount, this.minimum, newMaximum);
    }

    /** How much of the range is seen at once. */
    public int getVisibleAmount() {
        return this.visibleAmount;
    }

    /**
     * How much is seen at once.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getVisibleAmount}.
     */
    @Deprecated
    public int getVisible() {
        return this.visibleAmount;
    }

    /** Changes how much is seen at once. */
    public void setVisibleAmount(int newAmount) {
        this.setValues(this.value, newAmount, this.minimum, this.maximum);
    }

    /** How much it jumps with the arrows; never less than 1. */
    public void setUnitIncrement(int v) {
        this.setLineIncrement(v);
    }

    /**
     * How much it jumps with the arrows.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #setUnitIncrement}.
     */
    @Deprecated
    public synchronized void setLineIncrement(int v) {
        this.lineIncrement = Math.max(1, v);
    }

    /** How much it jumps with the arrows. */
    public int getUnitIncrement() {
        return this.lineIncrement;
    }

    /**
     * How much it jumps with the arrows.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getUnitIncrement}.
     */
    @Deprecated
    public int getLineIncrement() {
        return this.lineIncrement;
    }

    /** How much it jumps when the channel is pressed; never less than 1. */
    public void setBlockIncrement(int v) {
        this.setPageIncrement(v);
    }

    /**
     * How much it jumps when the channel is pressed.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #setBlockIncrement}.
     */
    @Deprecated
    public synchronized void setPageIncrement(int v) {
        this.pageIncrement = Math.max(1, v);
    }

    /** How much it jumps when the channel is pressed. */
    public int getBlockIncrement() {
        return this.pageIncrement;
    }

    /**
     * How much it jumps when the channel is pressed.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getBlockIncrement}.
     */
    @Deprecated
    public int getPageIncrement() {
        return this.pageIncrement;
    }

    /**
     * Changes the four measures at once.
     *
     * <p>It exists because changing them one at a time goes through impossible states —a value
     * outside the new range, a floor above the ceiling— and each step would clamp too much. Here
     * they are all adjusted together and only then is anything clamped.
     */
    public void setValues(int value, int visible, int minimum, int maximum) {
        synchronized (this) {
            if (minimum == Integer.MAX_VALUE) {
                minimum = Integer.MAX_VALUE - 1;
            }
            if (maximum <= minimum) {
                maximum = minimum + 1;
            }
            // The visible width cannot go past the range, nor be zero: a bar that shows nothing has
            // no thumb to hold.
            long maxSpan = (long) maximum - (long) minimum;
            if (maxSpan > Integer.MAX_VALUE) {
                maxSpan = Integer.MAX_VALUE;
            }
            if (visible > (int) maxSpan) {
                visible = (int) maxSpan;
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

    /** Whether the user is holding the thumb. */
    public boolean getValueIsAdjusting() {
        return this.isAdjusting;
    }

    /**
     * Says whether the user is holding the thumb.
     *
     * <p>It serves to avoid recomputing on every pixel of the drag: the listener can wait for this
     * to be `false` and do the expensive work just once.
     */
    public void setValueIsAdjusting(boolean b) {
        this.isAdjusting = b;
    }

    /** Adds a listener; `null` does nothing. */
    public synchronized void addAdjustmentListener(AdjustmentListener l) {
        if (l == null) {
            return;
        }
        this.adjustmentListener = AWTEventMulticaster.add(this.adjustmentListener, l);
        this.enableEvents(AWTEvent.ADJUSTMENT_EVENT_MASK);
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

    /** Tells the adjustment listeners. */
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

    /** The accessibility information of this bar. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTScrollBar();
        }
        return this.accessibleContext;
    }

    /**
     * The accessibility of a scrollbar.
     *
     * <p>The maximum it reports is {@link Scrollbar#getMaximum}, that is the **nominal** one, even
     * though the bar never reaches it. Reporting the reachable one would be more useful and would
     * be inventing: the JDK reports the nominal one, and it was checked.
     */
    protected class AccessibleAWTScrollBar extends AccessibleAWTComponent
            implements AccessibleValue {

        /** For the subclasses. */
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
         * Changes the value.
         *
         * @return `true` if the value was not `null`
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

        /** The nominal ceiling. */
        public Number getMaximumAccessibleValue() {
            return Integer.valueOf(Scrollbar.this.getMaximum());
        }
    }
}
