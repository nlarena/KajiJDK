package com.sun.jdi;

/**
 * A field of a type of the debugged machine.
 *
 * <p>The value is not here: it is asked for with {@code ObjectReference.getValue} for an
 * instance field or with {@code ReferenceType.getValue} for a static one.
 *
 * @since 1.3
 */
public interface Field extends TypeComponent, Comparable<Field> {

    /**
     * The type name.
     *
     * @return the result
     */
    String typeName();

    /**
     * The type.
     *
     * @return the result
     * @throws ClassNotLoadedException if it applies
     */
    Type type()
            throws ClassNotLoadedException;

    /**
     * Whether transient.
     *
     * @return the result
     */
    boolean isTransient();

    /**
     * Whether volatile.
     *
     * @return the result
     */
    boolean isVolatile();

    /**
     * Whether enum constant.
     *
     * @return the result
     */
    boolean isEnumConstant();

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
