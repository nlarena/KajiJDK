package javax.swing;

import java.io.Serializable;

/**
 * A sequence of numbers for a {@link JSpinner}.
 *
 * <h2>The type is ruled by the value, not by the bounds</h2>
 *
 * <p>If the value is an {@link Integer}, adding the step gives an {@code Integer}; if it is a
 * {@link Double}, it gives a {@code Double}. The bounds and the step may be of another type --
 * they are consulted with {@code compareTo} and with {@code doubleValue}/{@code longValue} --,
 * but the one that comes out of {@link #getNextValue} has the current value's type.
 *
 * <p>That matters more than it seems: a model that starts at {@code Integer.valueOf(0)} with a
 * step of {@code Double.valueOf(0.5)} advances by zero, because {@code longValue()} of 0.5 is 0.
 * It is not a mistake of the model; it is that the value's type decides the arithmetic.
 *
 * <h2>The bounds do not clip</h2>
 *
 * <p>{@link #setValue} accepts any number, even out of range. The bounds are only used in order
 * to decide whether there is a next or a previous one: on going past, the arrow returns null and
 * the control switches it off. See {@link SpinnerModel}'s note.
 */
public class SpinnerNumberModel extends AbstractSpinnerModel implements Serializable {

    private Number stepSize;
    private Number value;
    private Comparable<?> minimum;
    private Comparable<?> maximum;

    /**
     * With value, bounds and step.
     *
     * <p>The bounds may be null, which is like saying "no cap".
     *
     * @throws IllegalArgumentException if the value or the step are null, or if
     *     minimum &lt;= value &lt;= maximum does not hold.
     */
    public SpinnerNumberModel(Number value, Comparable<?> minimum, Comparable<?> maximum,
            Number stepSize) {
        if (!(value != null && stepSize != null)) {
            throw new IllegalArgumentException("value and stepSize must be non-null");
        }
        if (!((minimum == null || compare(minimum, value) <= 0)
                && (maximum == null || compare(maximum, value) >= 0))) {
            throw new IllegalArgumentException("(minimum <= value <= maximum) is false");
        }
        this.value = value;
        this.minimum = minimum;
        this.maximum = maximum;
        this.stepSize = stepSize;
    }

    /** With integers; the value comes out as an {@link Integer}. */
    public SpinnerNumberModel(int value, int minimum, int maximum, int stepSize) {
        this(Integer.valueOf(value), Integer.valueOf(minimum), Integer.valueOf(maximum),
                Integer.valueOf(stepSize));
    }

    /** With doubles; the value comes out as a {@link Double}. */
    public SpinnerNumberModel(double value, double minimum, double maximum, double stepSize) {
        this(Double.valueOf(value), Double.valueOf(minimum), Double.valueOf(maximum),
                Double.valueOf(stepSize));
    }

    /** From zero, one at a time, with no caps. */
    public SpinnerNumberModel() {
        this(Integer.valueOf(0), null, null, Integer.valueOf(1));
    }

    /**
     * It compares a bound with a value.
     *
     * <p>It concentrates the generic discard in a single place: the bound is declared
     * {@code Comparable<?>} and it can only be called by treating it as raw.
     */
    @SuppressWarnings("unchecked")
    private static int compare(Comparable<?> limit, Object value) {
        return ((Comparable<Object>) limit).compareTo(value);
    }

    /**
     * The floor; null removes the cap.
     *
     * <p>It is not checked against the current value: a floor may be set above the value, and what
     * happens then is that the down arrow is switched off.
     */
    public void setMinimum(Comparable<?> minimum) {
        if ((minimum == null) ? (this.minimum != null) : !minimum.equals(this.minimum)) {
            this.minimum = minimum;
            fireStateChanged();
        }
    }

    public Comparable<?> getMinimum() {
        return minimum;
    }

    /** The ceiling; null removes the cap. */
    public void setMaximum(Comparable<?> maximum) {
        if ((maximum == null) ? (this.maximum != null) : !maximum.equals(this.maximum)) {
            this.maximum = maximum;
            fireStateChanged();
        }
    }

    public Comparable<?> getMaximum() {
        return maximum;
    }

    /**
     * How much each arrow advances by.
     *
     * @throws IllegalArgumentException if it is null.
     */
    public void setStepSize(Number stepSize) {
        if (stepSize == null) {
            throw new IllegalArgumentException("null stepSize");
        }
        if (!stepSize.equals(this.stepSize)) {
            this.stepSize = stepSize;
            fireStateChanged();
        }
    }

    public Number getStepSize() {
        return stepSize;
    }

    /**
     * It adds the step in that direction.
     *
     * <p>The result's type is the current value's; see the class note. If the result goes outside
     * the bounds it returns null, which is how the model says "there is no more".
     */
    private Number run(int dir) {
        Number newValue;
        if ((value instanceof Float) || (value instanceof Double)) {
            double v = value.doubleValue() + (stepSize.doubleValue() * (double) dir);
            if (value instanceof Double) {
                newValue = Double.valueOf(v);
            } else {
                newValue = Float.valueOf((float) v);
            }
        } else {
            long v = value.longValue() + (stepSize.longValue() * (long) dir);
            if (value instanceof Long) {
                newValue = Long.valueOf(v);
            } else if (value instanceof Integer) {
                newValue = Integer.valueOf((int) v);
            } else if (value instanceof Short) {
                newValue = Short.valueOf((short) v);
            } else {
                newValue = Byte.valueOf((byte) v);
            }
        }
        if ((maximum != null) && compare(maximum, newValue) < 0) {
            return null;
        }
        if ((minimum != null) && compare(minimum, newValue) > 0) {
            return null;
        }
        return newValue;
    }

    /** The next one, or null if it goes past the ceiling. */
    public Object getNextValue() {
        return run(1);
    }

    /** The previous one, or null if it goes past the floor. */
    public Object getPreviousValue() {
        return run(-1);
    }

    /** The value, already as a number. */
    public Number getNumber() {
        return value;
    }

    public Object getValue() {
        return value;
    }

    /**
     * It changes the value.
     *
     * <p>It does not clip against the bounds; see the class note.
     *
     * @throws IllegalArgumentException if it is not a number.
     */
    public void setValue(Object value) {
        if ((value == null) || !(value instanceof Number)) {
            throw new IllegalArgumentException("illegal value");
        }
        if (!value.equals(this.value)) {
            this.value = (Number) value;
            fireStateChanged();
        }
    }
}
