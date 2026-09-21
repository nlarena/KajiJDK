package com.sun.jdi;

/**
 * A type's {@code Class} object, on the other side.
 *
 * @since 1.3
 */
public interface ClassObjectReference extends ObjectReference {

    /**
     * The reflected type.
     *
     * @return the result
     */
    ReferenceType reflectedType();
}
