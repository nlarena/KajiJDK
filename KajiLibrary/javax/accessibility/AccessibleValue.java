package javax.accessibility;

/**
 * Implemented by what represents a **number within a range**: a bar, a slider, a progress bar.
 *
 * <p>The three query methods return {@link Number} and not a concrete type because the range may be
 * integral or floating-point depending on the component, and forcing one of the two would mean
 * rounding in half the cases.
 */
public interface AccessibleValue {

    /** The current value. */
    Number getCurrentAccessibleValue();

    /**
     * Changes the value.
     *
     * @return `true` if it could
     */
    boolean setCurrentAccessibleValue(Number n);

    /** The smallest possible value. */
    Number getMinimumAccessibleValue();

    /** The largest possible value. */
    Number getMaximumAccessibleValue();
}
