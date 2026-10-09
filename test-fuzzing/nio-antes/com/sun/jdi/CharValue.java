package com.sun.jdi;

/**
 * CharValue of the debugged machine.
 *
 * @since 1.3
 */
public interface CharValue extends PrimitiveValue, Comparable<CharValue> {

    /**
     * The value.
     *
     * @return the result
     */
    char value();

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
