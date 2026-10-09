package com.sun.jdi;

/**
 * DoubleValue of the debugged machine.
 *
 * @since 1.3
 */
public interface DoubleValue extends PrimitiveValue, Comparable<DoubleValue> {

    /**
     * The value.
     *
     * @return the result
     */
    double value();

    /**
     * Two mirrors are equal if they name the same thing in the same VM.
     *
     * @param obj the Object
     * @return the result
     */
    boolean equals(Object obj);

    /**
     * Consistent with {@link #equals}.
     *
     * @return the result
     */
    int hashCode();
}
