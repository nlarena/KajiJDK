package com.sun.jdi;

/**
 * A lock a thread holds.
 *
 * @since 1.3
 */
public interface MonitorInfo extends Mirror {

    /**
     * The monitor.
     *
     * @return the result
     */
    ObjectReference monitor();

    /**
     * The stack depth.
     *
     * @return the result
     */
    int stackDepth();

    /**
     * The thread.
     *
     * @return the result
     */
    ThreadReference thread();
}
