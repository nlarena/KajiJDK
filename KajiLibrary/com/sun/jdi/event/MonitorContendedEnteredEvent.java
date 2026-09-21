package com.sun.jdi.event;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;

/**
 * A thread got the lock it was waiting for.
 *
 * @since 1.3
 */
public interface MonitorContendedEnteredEvent extends LocatableEvent {

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
