package com.sun.jdi;

import java.util.List;

/**
 * A method of a type of the debugged machine.
 *
 * <p>{@code arguments} needs the local variable table and it may not be there;
 * {@code argumentTypeNames} comes from the signature and is always there. It is the difference
 * between knowing what the parameters were called and knowing what type they were.
 *
 * <p>{@code isObsolete} marks a method whose code was replaced on the fly: the reference still
 * holds and the code it represented no longer exists.
 *
 * @since 1.3
 */
public interface Method extends TypeComponent, Locatable, Comparable<Method> {

    /**
     * The return type name.
     *
     * @return the result
     */
    String returnTypeName();

    /**
     * The return type.
     *
     * @return the result
     * @throws ClassNotLoadedException if it applies
     */
    Type returnType()
            throws ClassNotLoadedException;

    /**
     * The argument type names.
     *
     * @return the result
     */
    List<String> argumentTypeNames();

    /**
     * The argument types.
     *
     * @return the result
     * @throws ClassNotLoadedException if it applies
     */
    List<Type> argumentTypes()
            throws ClassNotLoadedException;

    /**
     * Whether abstract.
     *
     * @return the result
     */
    boolean isAbstract();

    /**
     * Whether default.
     *
     * @return the result
     */
    boolean isDefault();

    /**
     * Whether synchronized.
     *
     * @return the result
     */
    boolean isSynchronized();

    /**
     * Whether native.
     *
     * @return the result
     */
    boolean isNative();

    /**
     * Whether var args.
     *
     * @return the result
     */
    boolean isVarArgs();

    /**
     * Whether bridge.
     *
     * @return the result
     */
    boolean isBridge();

    /**
     * Whether constructor.
     *
     * @return the result
     */
    boolean isConstructor();

    /**
     * Whether static initializer.
     *
     * @return the result
     */
    boolean isStaticInitializer();

    /**
     * Whether obsolete.
     *
     * @return the result
     */
    boolean isObsolete();

    /**
     * Every line locations, the inherited ones included.
     *
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<Location> allLineLocations()
            throws AbsentInformationException;

    /**
     * Every line locations, the inherited ones included.
     *
     * @param name the String
     * @param name2 the String
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<Location> allLineLocations(String name, String name2)
            throws AbsentInformationException;

    /**
     * The locations of line.
     *
     * @param index the int
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<Location> locationsOfLine(int index)
            throws AbsentInformationException;

    /**
     * The locations of line.
     *
     * @param name the String
     * @param name2 the String
     * @param index the int
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<Location> locationsOfLine(String name, String name2, int index)
            throws AbsentInformationException;

    /**
     * The location of code index.
     *
     * @param index the long
     * @return the result
     */
    Location locationOfCodeIndex(long index);

    /**
     * The variables.
     *
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<LocalVariable> variables()
            throws AbsentInformationException;

    /**
     * The variables by name.
     *
     * @param name the String
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<LocalVariable> variablesByName(String name)
            throws AbsentInformationException;

    /**
     * The arguments.
     *
     * @return the result
     * @throws AbsentInformationException if it applies
     */
    List<LocalVariable> arguments()
            throws AbsentInformationException;

    /**
     * The bytecodes.
     *
     * @return the result
     */
    byte[] bytecodes();

    /**
     * The location.
     *
     * @return the result
     */
    Location location();

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
