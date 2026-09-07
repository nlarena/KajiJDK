package com.sun.jdi;

import java.util.List;
import java.util.Map;

/**
 * Un objeto de la maquina depurada, visto desde afuera.
 *
 * <p>No es el objeto: es un identificador con el que preguntarle cosas. Leer un campo o llamar un
 * metodo son viajes a la otra VM.
 *
 * <p>{@code invokeMethod} tiene una condicion que hay que entender antes de usarlo: el hilo que se
 * le pasa <strong>tiene que estar suspendido</strong>, y la llamada se ejecuta en ese hilo. Puede
 * tomar candados y cambiar el estado del programa depurado. No es una consulta: es ejecutar codigo
 * del otro lado.
 *
 * <p>{@code disableCollection} existe porque entre que se obtiene una referencia y se la usa, el
 * recolector de la otra VM puede llevarse el objeto.
 *
 * @since 1.3
 */
public interface ObjectReference extends Value {

    /**
     * Al invocar, dejar suspendidos los demas hilos.
     *
     * <p>Suena seguro y casi siempre es lo contrario: si el metodo que se invoca necesita un
     * candado que tiene otro hilo, ese hilo esta suspendido y la invocacion no vuelve nunca.
     */
    int INVOKE_SINGLE_THREADED = 1;

    /**
     * Llamar al metodo tal como se lo nombro, sin despacho virtual: el equivalente de
     * {@code super.metodo()}.
     */
    int INVOKE_NONVIRTUAL = 2;

    /**
     * El reference type.
     *
     * @return el resultado
     */
    ReferenceType referenceType();

    /**
     * El value.
     *
     * @param field el Field
     * @return el resultado
     */
    Value getValue(Field field);

    /**
     * El values.
     *
     * @param values el List<? extends Field>
     * @return el resultado
     */
    Map<Field, Value> getValues(List<? extends Field> values);

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
     * El disable collection.
     */
    void disableCollection();

    /**
     * El enable collection.
     */
    void enableCollection();

    /**
     * Si collected.
     *
     * @return el resultado
     */
    boolean isCollected();

    /**
     * El unique i d.
     *
     * @return el resultado
     */
    long uniqueID();

    /**
     * El waiting threads.
     *
     * @return el resultado
     * @throws IncompatibleThreadStateException si corresponde
     */
    List<ThreadReference> waitingThreads()
            throws IncompatibleThreadStateException;

    /**
     * El owning thread.
     *
     * @return el resultado
     * @throws IncompatibleThreadStateException si corresponde
     */
    ThreadReference owningThread()
            throws IncompatibleThreadStateException;

    /**
     * El entry count.
     *
     * @return el resultado
     * @throws IncompatibleThreadStateException si corresponde
     */
    int entryCount()
            throws IncompatibleThreadStateException;

    /**
     * El referring objects.
     *
     * @param index el long
     * @return el resultado
     */
    List<ObjectReference> referringObjects(long index);

    /**
     * Dos reflejos son iguales si nombran a lo mismo en la misma VM.
     *
     * @param obj el Object
     * @return el resultado
     */
    boolean equals(Object obj);

    /**
     * Coherente con {@link #equals}.
     *
     * @return el resultado
     */
    int hashCode();
}
