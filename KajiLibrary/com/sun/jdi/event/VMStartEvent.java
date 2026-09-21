package com.sun.jdi.event;

import com.sun.jdi.ThreadReference;

/**
 * The debugged machine started and is ready.
 *
 * <p>It arrives with the main thread suspended before the first instruction, which is the only
 * window for configuring things before the program does anything.
 *
 * @since 1.3
 */
public interface VMStartEvent extends Event {

    /**
     * The thread.
     *
     * @return the result
     */
    ThreadReference thread();
}
