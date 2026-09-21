package com.sun.jdi;

/**
 * A primitive value, with the datum inside.
 *
 * <p>The conversion methods do Java's widening: reading a {@code byte} as an {@code int}
 * works, the other way round does not.
 *
 * @since 1.3
 */
public interface PrimitiveValue extends Value {

    /**
     * The boolean value.
     *
     * @return the result
     */
    boolean booleanValue();

    /**
     * The byte value.
     *
     * @return the result
     */
    byte byteValue();

    /**
     * The char value.
     *
     * @return the result
     */
    char charValue();

    /**
     * The short value.
     *
     * @return the result
     */
    short shortValue();

    /**
     * The int value.
     *
     * @return the result
     */
    int intValue();

    /**
     * The long value.
     *
     * @return the result
     */
    long longValue();

    /**
     * The float value.
     *
     * @return the result
     */
    float floatValue();

    /**
     * The double value.
     *
     * @return the result
     */
    double doubleValue();
}
