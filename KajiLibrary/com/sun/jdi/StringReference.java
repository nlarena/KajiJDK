package com.sun.jdi;

/**
 * A string of the debugged machine.
 *
 * @since 1.3
 */
public interface StringReference extends ObjectReference {

    /**
     * The value.
     *
     * @return the result
     */
    String value();
}
