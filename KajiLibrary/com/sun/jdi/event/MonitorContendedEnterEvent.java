package com.sun.jdi.event;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;

/**
 * A thread began waiting for a lock another one holds.
 *
 * @since 1.3
 */
public interface MonitorContendedEnterEvent extends LocatableEvent {

    /**
     * The thread.
     *
     * @return the result
     */
    ThreadReference thread();

    /**
     * The monitor.
     *
     * @return the result
     */
    ObjectReference monitor();
}
