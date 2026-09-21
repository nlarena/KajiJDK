package com.sun.jdi;

/**
 * A local variable of a method.
 *
 * @since 1.3
 */
public interface LocalVariable extends Mirror, Comparable<LocalVariable> {

    /**
     * The name.
     *
     * @return the result
     */
    String name();

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
     * The signature.
     *
     * @return the result
     */
    String signature();

    /**
     * The generic signature.
     *
     * @return the result
     */
    String genericSignature();

    /**
     * Whether visible.
     *
     * @param frame the StackFrame
     * @return the result
     */
    boolean isVisible(StackFrame frame);

    /**
     * Whether argument.
     *
     * @return the result
     */
    boolean isArgument();

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
