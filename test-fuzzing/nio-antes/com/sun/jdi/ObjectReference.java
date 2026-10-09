package com.sun.jdi;

import java.util.List;
import java.util.Map;

/**
 * An object of the debugged machine, seen from outside.
 *
 * <p>It is not the object: it is an identifier to ask it things with. Reading a field or
 * calling a method are trips to the other VM.
 *
 * <p>{@code invokeMethod} has a condition that has to be understood before using it: the thread
 * it is passed <strong>has to be suspended</strong>, and the call is executed on that thread.
 * It may take locks and change the debugged program's state. It is not a query: it is executing
 * code on the other side.
 *
 * <p>{@code disableCollection} exists because between obtaining a reference and using it, the
 * other VM's collector may take the object away.
 *
 * @since 1.3
 */
public interface ObjectReference extends Value {

    /**
     * On invoking, leave the other threads suspended.
     *
     * <p>It sounds safe and it is almost always the opposite: if the method that is invoked
     * needs a lock another thread holds, that thread is suspended and the invocation never
     * returns.
     */
    int INVOKE_SINGLE_THREADED = 1;

    /**
     * Call the method just as it was named, with no virtual dispatch: the equivalent of
     * {@code super.method()}.
     */
    int INVOKE_NONVIRTUAL = 2;

    /**
     * The reference type.
     *
     * @return the result
     */
    ReferenceType referenceType();

    /**
     * The value.
     *
     * @param field the Field
     * @return the result
     */
    Value getValue(Field field);

    /**
     * The values.
     *
     * @param values the List<? extends Field>
     * @return the result
     */
    Map<Field, Value> getValues(List<? extends Field> values);

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
     * The disable collection.
     */
    void disableCollection();

    /**
     * The enable collection.
     */
    void enableCollection();

    /**
     * Whether collected.
     *
     * @return the result
     */
    boolean isCollected();

    /**
     * The unique i d.
     *
     * @return the result
     */
    long uniqueID();

    /**
     * The waiting threads.
     *
     * @return the result
     * @throws IncompatibleThreadStateException if it applies
     */
    List<ThreadReference> waitingThreads()
            throws IncompatibleThreadStateException;

    /**
     * The owning thread.
     *
     * @return the result
     * @throws IncompatibleThreadStateException if it applies
     */
    ThreadReference owningThread()
            throws IncompatibleThreadStateException;

    /**
     * The entry count.
     *
     * @return the result
     * @throws IncompatibleThreadStateException if it applies
     */
    int entryCount()
            throws IncompatibleThreadStateException;

    /**
     * The referring objects.
     *
     * @param index the long
     * @return the result
     */
    List<ObjectReference> referringObjects(long index);

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
