package com.sun.jdi.event;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;

/**
 * Un hilo empezo a esperar por un candado que otro tiene.
 *
 * @since 1.3
 */
public interface MonitorContendedEnterEvent extends LocatableEvent {

    /**
     * El thread.
     *
     * @return el resultado
     */
    ThreadReference thread();

    /**
     * El monitor.
     *
     * @return el resultado
     */
    ObjectReference monitor();
}
