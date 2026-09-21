package com.sun.jdi.event;

import com.sun.jdi.ThreadReference;

/**
 * A thread ended.
 *
 * @since 1.3
 */
public interface ThreadDeathEvent extends Event {

    /**
     * The thread.
     *
     * @return the result
     */
    ThreadReference thread();
}
