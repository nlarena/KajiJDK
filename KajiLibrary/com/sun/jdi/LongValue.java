package com.sun.jdi;

/**
 * LongValue of the debugged machine.
 *
 * @since 1.3
 */
public interface LongValue extends PrimitiveValue, Comparable<LongValue> {

    /**
     * The value.
     *
     * @return the result
     */
    long value();

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
