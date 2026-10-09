package com.sun.jdi;

/**
 * ByteValue of the debugged machine.
 *
 * @since 1.3
 */
public interface ByteValue extends PrimitiveValue, Comparable<ByteValue> {

    /**
     * The value.
     *
     * @return the result
     */
    byte value();

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
