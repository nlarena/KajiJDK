package com.sun.jdi;

/**
 * A point in the code: a class, a method and a bytecode index.
 *
 * <p>{@code lineNumber} and {@code sourceName} may not be there: they depend on the class
 * having been compiled with the debugging information, which is optional. When it is missing
 * they throw {@link AbsentInformationException} and the only thing that places the point is
 * {@code codeIndex}.
 *
 * @since 1.3
 */
public interface Location extends Mirror, Comparable<Location> {

    /**
     * The declaring type.
     *
     * @return the result
     */
    ReferenceType declaringType();

    /**
     * The method.
     *
     * @return the result
     */
    Method method();

    /**
     * The code index.
     *
     * @return the result
     */
    long codeIndex();

    /**
     * The source name.
     *
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    String sourceName()
            throws AbsentInformationException;

    /**
     * The source name.
     *
     * @param name the String
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    String sourceName(String name)
            throws AbsentInformationException;

    /**
     * The source path.
     *
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    String sourcePath()
            throws AbsentInformationException;

    /**
     * The source path.
     *
     * @param name the String
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    String sourcePath(String name)
            throws AbsentInformationException;

    /**
     * The line number.
     *
     * @return the result
     */
    int lineNumber();

    /**
     * The line number.
     *
     * @param name the String
     * @return the result
     */
    int lineNumber(String name);

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
