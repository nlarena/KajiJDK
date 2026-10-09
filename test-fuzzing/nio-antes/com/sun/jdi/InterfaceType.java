package com.sun.jdi;

import java.util.List;

/**
 * An interface of the debugged machine.
 *
 * @since 1.3
 */
public interface InterfaceType extends ReferenceType {

    /**
     * The superinterfaces.
     *
     * @return the result
     */
    List<InterfaceType> superinterfaces();

    /**
     * The subinterfaces.
     *
     * @return the result
     */
    List<InterfaceType> subinterfaces();

    /**
     * The implementors.
     *
     * @return the result
     */
    List<ClassType> implementors();

    /**
     * The invoke method.
     *
     * @param thread the ThreadReference
     * @param method the Method
     * @param values the List<? extends Value>
     * @param index the int
     * @return the result
     * @throws InvalidTypeException if it applies
     * @throws ClassNotLoadedException if it applies
     * @throws IncompatibleThreadStateException if it applies
     * @throws InvocationException if it applies
     */
    Value invokeMethod(
            ThreadReference thread, Method method, List<? extends Value> values, int index)
            throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException;
}
