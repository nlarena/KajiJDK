package com.sun.jdi;

import java.util.List;

/**
 * Una clase de la maquina depurada.
 *
 * @since 1.3
 */
public interface ClassType extends ReferenceType {

    /**
     * Al invocar, dejar suspendidos los demas hilos.
     *
     * <p>Suena seguro y casi siempre es lo contrario: si el metodo que se invoca necesita un
     * candado que tiene otro hilo, ese hilo esta suspendido y la invocacion no vuelve nunca.
     */
    int INVOKE_SINGLE_THREADED = 1;

    /**
     * El superclass.
     *
     * @return el resultado
     */
    ClassType superclass();

    /**
     * El interfaces.
     *
     * @return el resultado
     */
    List<InterfaceType> interfaces();

    /**
     * Todos los interfaces, heredados incluidos.
     *
     * @return el resultado
     */
    List<InterfaceType> allInterfaces();

    /**
     * El subclasses.
     *
     * @return el resultado
     */
    List<ClassType> subclasses();

    /**
     * Si enum.
     *
     * @return el resultado
     */
    boolean isEnum();

    /**
     * Fija el value.
     *
     * @param field el Field
     * @param value el Value
     * @throws InvalidTypeException si corresponde
     * @throws ClassNotLoadedException si corresponde
     */
    void setValue(Field field, Value value)
            throws InvalidTypeException, ClassNotLoadedException;

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

    /**
     * El new instance.
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
    ObjectReference newInstance(
            ThreadReference thread, Method method, List<? extends Value> values, int index)
            throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException;

    /**
     * El concrete method by name.
     *
     * @param name el String
     * @param name2 el String
     * @return el resultado
     */
    Method concreteMethodByName(String name, String name2);
}
