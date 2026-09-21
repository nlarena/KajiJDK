package com.sun.jdi.event;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;

/**
 * A thread entered a {@code wait}.
 *
 * @since 1.3
 */
public interface MonitorWaitEvent extends LocatableEvent {

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

    /**
     * The timeout.
     *
     * @return the result
     */
    long timeout();
}
