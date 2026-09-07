package com.sun.jdi.event;

import com.sun.jdi.ThreadReference;

/**
 * Arranco un hilo.
 *
 * @since 1.3
 */
public interface ThreadStartEvent extends Event {

    /**
     * El thread.
     *
     * @return el resultado
     */
    ThreadReference thread();
}
