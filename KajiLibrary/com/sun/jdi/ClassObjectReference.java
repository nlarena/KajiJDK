package com.sun.jdi;

/**
 * El objeto {@code Class} de un tipo, del otro lado.
 *
 * @since 1.3
 */
public interface ClassObjectReference extends ObjectReference {

    /**
     * El reflected type.
     *
     * @return el resultado
     */
    ReferenceType reflectedType();
}
