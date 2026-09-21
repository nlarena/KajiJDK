package com.sun.jdi;

/**
 * A value of the debugged machine.
 *
 * <p>It splits into two branches that do not mix: {@link PrimitiveValue}, which carries the
 * datum inside, and {@link ObjectReference}, which carries an identifier. An {@code int}
 * travels; an object does not.
 *
 * @since 1.3
 */
public interface Value extends Mirror {

    /**
     * The type.
     *
     * @return the result
     */
    Type type();
}
