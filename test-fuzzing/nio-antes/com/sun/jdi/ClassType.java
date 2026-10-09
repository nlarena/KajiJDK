package com.sun.jdi;

import java.util.List;

/**
 * A class of the debugged machine.
 *
 * @since 1.3
 */
public interface ClassType extends ReferenceType {

    /**
     * On invoking, leave the other threads suspended.
     *
     * <p>It sounds safe and it is almost always the opposite: if the method that is invoked needs a
     * lock another thread holds, that thread is suspended and the invocation never returns.
     */
    int INVOKE_SINGLE_THREADED = 1;

    /**
     * The superclass.
     *
     * @return the result
     */
    ClassType superclass();

    /**
     * The interfaces.
     *
     * @return the result
     */
    List<InterfaceType> interfaces();

    /**
     * Every interfaces, the inherited ones included.
     *
     * @return the result
     */
    List<InterfaceType> allInterfaces();

    /**
     * The subclasses.
     *
     * @return the result
     */
    List<ClassType> subclasses();

    /**
     * Whether enum.
     *
     * @return the result
     */
    boolean isEnum();

    /**
     * It fixes the value.
     *
     * @param field the Field
     * @param value the Value
     * @throws InvalidTypeException if it applies
     * @throws ClassNotLoadedException if it applies
     */
    void setValue(Field field, Value value)
            throws InvalidTypeException, ClassNotLoadedException;

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

    /**
     * The new instance.
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
    ObjectReference newInstance(
            ThreadReference thread, Method method, List<? extends Value> values, int index)
            throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException;

    /**
     * The concrete method by name.
     *
     * @param name the String
     * @param name2 the String
     * @return the result
     */
    Method concreteMethodByName(String name, String name2);
}
