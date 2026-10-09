package com.sun.jdi;

import java.util.List;

/**
 * A thread of the debugged machine.
 *
 * <p>Almost everything interesting -- the stack, the local variables, the current frame -- can
 * only be read with the thread <strong>suspended</strong>; with it running they throw
 * {@link IncompatibleThreadStateException}. It is not a limitation of the API: a stack that
 * moves while it is being read means nothing.
 *
 * <p>{@code suspend} and {@code resume} carry a <strong>count</strong>, not a flag: two
 * suspensions need two resumptions. It is what allows two observers to suspend the same thread
 * without getting in each other's way, and also the commonest cause of a thread that never
 * starts again.
 *
 * @since 1.3
 */
public interface ThreadReference extends ObjectReference {

    /** The thread's state could not be determined. */
    int THREAD_STATUS_UNKNOWN = -1;

    /** The thread ended. */
    int THREAD_STATUS_ZOMBIE = 0;

    /** The thread is running. */
    int THREAD_STATUS_RUNNING = 1;

    /** The thread is in a {@code sleep}. */
    int THREAD_STATUS_SLEEPING = 2;

    /** The thread is waiting to enter a synchronized block. */
    int THREAD_STATUS_MONITOR = 3;

    /** The thread is in a {@code wait}. */
    int THREAD_STATUS_WAIT = 4;

    /** The thread has not started yet. */
    int THREAD_STATUS_NOT_STARTED = 5;

    /**
     * The name.
     *
     * @return the result
     */
    String name();

    /**
     * The suspend.
     */
    void suspend();

    /**
     * The resume.
     */
    void resume();

    /**
     * The suspend count.
     *
     * @return the result
     */
    int suspendCount();

    /**
     * The stop.
     *
     * @param object the ObjectReference
     * @throws InvalidTypeException if it applies
     */
    void stop(ObjectReference object)
            throws InvalidTypeException;

    /**
     * The interrupt.
     */
    void interrupt();

    /**
     * The status.
     *
     * @return the result
     */
    int status();

    /**
     * Whether suspended.
     *
     * @return the result
     */
    boolean isSuspended();

    /**
     * Whether at breakpoint.
     *
     * @return the result
     */
    boolean isAtBreakpoint();

    /**
     * The thread group.
     *
     * @return the result
     */
    ThreadGroupReference threadGroup();

    /**
     * The frame count.
     *
     * @return the result
     * @throws IncompatibleThreadStateException if it applies
     */
    int frameCount()
            throws IncompatibleThreadStateException;

    /**
     * The frames.
     *
     * @return the result
     * @throws IncompatibleThreadStateException if it applies
     */
    List<StackFrame> frames()
            throws IncompatibleThreadStateException;

    /**
     * The frame.
     *
     * @param index the int
     * @return the result
     * @throws IncompatibleThreadStateException if it applies
     */
    StackFrame frame(int index)
            throws IncompatibleThreadStateException;

    /**
     * The frames.
     *
     * @param index the int
     * @param index2 the int
     * @return the result
     * @throws IncompatibleThreadStateException if it applies
     */
    List<StackFrame> frames(int index, int index2)
            throws IncompatibleThreadStateException;

    /**
     * The owned monitors.
     *
     * @return the result
     * @throws IncompatibleThreadStateException if it applies
     */
    List<ObjectReference> ownedMonitors()
            throws IncompatibleThreadStateException;

    /**
     * The owned monitors and frames.
     *
     * @return the result
     * @throws IncompatibleThreadStateException if it applies
     */
    List<MonitorInfo> ownedMonitorsAndFrames()
            throws IncompatibleThreadStateException;

    /**
     * The current contended monitor.
     *
     * @return the result
     * @throws IncompatibleThreadStateException if it applies
     */
    ObjectReference currentContendedMonitor()
            throws IncompatibleThreadStateException;

    /**
     * The pop frames.
     *
     * @param frame the StackFrame
     * @throws IncompatibleThreadStateException if it applies
     */
    void popFrames(StackFrame frame)
            throws IncompatibleThreadStateException;

    /**
     * The force early return.
     *
     * @param value the Value
     * @throws InvalidTypeException if it applies
     * @throws ClassNotLoadedException if it applies
     * @throws IncompatibleThreadStateException if it applies
     */
    void forceEarlyReturn(Value value)
            throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException;

    /**
     * Whether virtual.
     *
     * @return the result
     */
    boolean isVirtual();
}
