package com.sun.jdi;

/**
 * A type of the debugged machine.
 *
 * <p>{@link PrimitiveType} for the eight primitives and {@link ReferenceType} for everything
 * else. The difference matters because only a {@code ReferenceType} has members, code and a
 * class loader behind it.
 *
 * @since 1.3
 */
public interface Type extends Mirror {

    /**
     * The signature.
     *
     * @return the result
     */
    String signature();

    /**
     * The name.
     *
     * @return the result
     */
    String name();
}
