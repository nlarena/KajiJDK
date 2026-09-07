package java.awt.event;

import java.awt.Component;

/**
 * The mouse wheel was moved.
 *
 * <p>The wheel does not report pixels but **notches**, and what a notch is worth is decided by the
 * system, not by the application. That is why there are two numbers: the rotation --how many
 * notches-- and the scroll amount --how many units per notch the user wants, according to their
 * settings. {@link #getUnitsToScroll} multiplies the two and is what is nearly always wanted.
 *
 * <p>{@link #getPreciseWheelRotation} exists for the wheels and trackpads that report fractions: with
 * those {@link #getWheelRotation} rounds and a smooth scroll comes out in jumps.
 */
public class MouseWheelEvent extends MouseEvent {

    private static final long serialVersionUID = 6459879390515399677L;

    /** Scroll by blocks: one screenful per notch. */
    public static final int WHEEL_BLOCK_SCROLL = 1;

    /** Scroll by units, according to the user's settings. */
    public static final int WHEEL_UNIT_SCROLL = 0;

    private final int scrollType;
    private final int scrollAmount;
    private final int wheelRotation;
    private final double preciseWheelRotation;

    /**
     * With the whole rotation.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public MouseWheelEvent(Component source, int id, long when, int modifiers, int x, int y,
            int clickCount, boolean popupTrigger, int scrollType, int scrollAmount,
            int wheelRotation) {
        this(source, id, when, modifiers, x, y, 0, 0, clickCount, popupTrigger, scrollType,
                scrollAmount, wheelRotation, wheelRotation);
    }

    /**
     * Like the previous one, with the position on screen.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public MouseWheelEvent(Component source, int id, long when, int modifiers, int x, int y,
            int xAbs, int yAbs, int clickCount, boolean popupTrigger, int scrollType,
            int scrollAmount, int wheelRotation) {
        this(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger, scrollType,
                scrollAmount, wheelRotation, wheelRotation);
    }

    /**
     * With the fractional rotation.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public MouseWheelEvent(Component source, int id, long when, int modifiers, int x, int y,
            int xAbs, int yAbs, int clickCount, boolean popupTrigger, int scrollType,
            int scrollAmount, int wheelRotation, double preciseWheelRotation) {
        super(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger,
                MouseEvent.NOBUTTON);
        this.scrollType = scrollType;
        this.scrollAmount = scrollAmount;
        this.wheelRotation = wheelRotation;
        this.preciseWheelRotation = preciseWheelRotation;
    }

    /** By units or by blocks. */
    public int getScrollType() {
        return this.scrollType;
    }

    /** How many units per notch the user wants. */
    public int getScrollAmount() {
        return this.scrollAmount;
    }

    /** How many notches it moved; negative is upwards. */
    public int getWheelRotation() {
        return this.wheelRotation;
    }

    /** How many notches it moved, with fractions. */
    public double getPreciseWheelRotation() {
        return this.preciseWheelRotation;
    }

    /**
     * How many units have to be scrolled.
     *
     * <p>It multiplies whatever the scroll type is, as the JDK does. The number only means something
     * when the type is {@link #WHEEL_UNIT_SCROLL}: with blocks the unit is a screenful and this
     * product does not describe it.
     *
     * @return the notches times the units
     */
    public int getUnitsToScroll() {
        return this.scrollAmount * this.wheelRotation;
    }

    public String paramString() {
        String type;
        if (this.scrollType == WHEEL_UNIT_SCROLL) {
            type = "WHEEL_UNIT_SCROLL";
        } else if (this.scrollType == WHEEL_BLOCK_SCROLL) {
            type = "WHEEL_BLOCK_SCROLL";
        } else {
            type = "unknown scroll type";
        }
        return super.paramString() + ",scrollType=" + type + ",scrollAmount=" + this.scrollAmount
                + ",wheelRotation=" + this.wheelRotation + ",preciseWheelRotation="
                + this.preciseWheelRotation;
    }
}
