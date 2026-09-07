package com.sun.jdi;

import java.util.List;

/**
 * Un hilo de la maquina depurada.
 *
 * <p>Casi todo lo interesante --la pila, las variables locales, el marco actual-- solo se puede
 * leer con el hilo <strong>suspendido</strong>; con el corriendo tiran
 * {@link IncompatibleThreadStateException}. No es una limitacion de la API: una pila que se mueve
 * mientras se la lee no significa nada.
 *
 * <p>{@code suspend} y {@code resume} llevan una <strong>cuenta</strong>, no una bandera: dos
 * suspensiones necesitan dos reanudaciones. Es lo que permite que dos observadores suspendan el
 * mismo hilo sin pisarse, y tambien la causa mas comun de un hilo que no arranca nunca.
 *
 * @since 1.3
 */
public interface ThreadReference extends ObjectReference {

    /** No se pudo determinar el estado del hilo. */
    int THREAD_STATUS_UNKNOWN = -1;

    /** El hilo termino. */
    int THREAD_STATUS_ZOMBIE = 0;

    /** El hilo esta corriendo. */
    int THREAD_STATUS_RUNNING = 1;

    /** El hilo esta en un {@code sleep}. */
    int THREAD_STATUS_SLEEPING = 2;

    /** El hilo espera para entrar a un bloque sincronizado. */
    int THREAD_STATUS_MONITOR = 3;

    /** El hilo esta en un {@code wait}. */
    int THREAD_STATUS_WAIT = 4;

    /** El hilo todavia no arranco. */
    int THREAD_STATUS_NOT_STARTED = 5;

    /**
     * El nombre.
     *
     * @return el resultado
     */
    String name();

    /**
     * El suspend.
     */
    void suspend();

    /**
     * El resume.
     */
    void resume();

    /**
     * El suspend count.
     *
     * @return el resultado
     */
    int suspendCount();

    /**
     * El stop.
     *
     * @param object el ObjectReference
     * @throws InvalidTypeException si corresponde
     */
    void stop(ObjectReference object)
            throws InvalidTypeException;

    /**
     * El interrupt.
     */
    void interrupt();

    /**
     * El status.
     *
     * @return el resultado
     */
    int status();

    /**
     * Si suspended.
     *
     * @return el resultado
     */
    boolean isSuspended();

    /**
     * Si at breakpoint.
     *
     * @return el resultado
     */
    boolean isAtBreakpoint();

    /**
     * El thread group.
     *
     * @return el resultado
     */
    ThreadGroupReference threadGroup();

    /**
     * El frame count.
     *
     * @return el resultado
     * @throws IncompatibleThreadStateException si corresponde
     */
    int frameCount()
            throws IncompatibleThreadStateException;

    /**
     * El frames.
     *
     * @return el resultado
     * @throws IncompatibleThreadStateException si corresponde
     */
    List<StackFrame> frames()
            throws IncompatibleThreadStateException;

    /**
     * El frame.
     *
     * @param index el int
     * @return el resultado
     * @throws IncompatibleThreadStateException si corresponde
     */
    StackFrame frame(int index)
            throws IncompatibleThreadStateException;

    /**
     * El frames.
     *
     * @param index el int
     * @param index2 el int
     * @return el resultado
     * @throws IncompatibleThreadStateException si corresponde
     */
    List<StackFrame> frames(int index, int index2)
            throws IncompatibleThreadStateException;

    /**
     * El owned monitors.
     *
     * @return el resultado
     * @throws IncompatibleThreadStateException si corresponde
     */
    List<ObjectReference> ownedMonitors()
            throws IncompatibleThreadStateException;

    /**
     * El owned monitors and frames.
     *
     * @return el resultado
     * @throws IncompatibleThreadStateException si corresponde
     */
    List<MonitorInfo> ownedMonitorsAndFrames()
            throws IncompatibleThreadStateException;

    /**
     * El current contended monitor.
     *
     * @return el resultado
     * @throws IncompatibleThreadStateException si corresponde
     */
    ObjectReference currentContendedMonitor()
            throws IncompatibleThreadStateException;

    /**
     * El pop frames.
     *
     * @param frame el StackFrame
     * @throws IncompatibleThreadStateException si corresponde
     */
    void popFrames(StackFrame frame)
            throws IncompatibleThreadStateException;

    /**
     * El force early return.
     *
     * @param value el Value
     * @throws InvalidTypeException si corresponde
     * @throws ClassNotLoadedException si corresponde
     * @throws IncompatibleThreadStateException si corresponde
     */
    void forceEarlyReturn(Value value)
            throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException;

    /**
     * Si virtual.
     *
     * @return el resultado
     */
    boolean isVirtual();
}
