package com.sun.jdi;

import java.util.List;

/**
 * Una interfaz de la maquina depurada.
 *
 * @since 1.3
 */
public interface InterfaceType extends ReferenceType {

    /**
     * El superinterfaces.
     *
     * @return el resultado
     */
    List<InterfaceType> superinterfaces();

    /**
     * El subinterfaces.
     *
     * @return el resultado
     */
    List<InterfaceType> subinterfaces();

    /**
     * El implementors.
     *
     * @return el resultado
     */
    List<ClassType> implementors();

    /**
     * El invoke method.
     *
     * @param thread el ThreadReference
     * @param method el Method
     * @param values el List<? extends Value>
     * @param index el int
     * @return el resultado
     * @throws InvalidTypeException si corresponde
     * @throws ClassNotLoadedException si corresponde
     * @throws IncompatibleThreadStateException si corresponde
     * @throws InvocationException si corresponde
     */
    Value invokeMethod(
            ThreadReference thread, Method method, List<? extends Value> values, int index)
            throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException;
}
