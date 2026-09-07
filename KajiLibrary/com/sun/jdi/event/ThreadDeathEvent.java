package com.sun.jdi.event;

import com.sun.jdi.ThreadReference;

/**
 * Termino un hilo.
 *
 * @since 1.3
 */
public interface ThreadDeathEvent extends Event {

    /**
     * El thread.
     *
     * @return el resultado
     */
    ThreadReference thread();
}
