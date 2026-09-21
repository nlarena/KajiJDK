package com.sun.jdi.event;

import com.sun.jdi.ThreadReference;

/**
 * A thread started.
 *
 * @since 1.3
 */
public interface ThreadStartEvent extends Event {

    /**
     * The thread.
     *
     * @return the result
     */
    ThreadReference thread();
}
