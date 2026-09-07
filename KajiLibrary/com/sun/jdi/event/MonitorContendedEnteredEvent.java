package com.sun.jdi.event;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;

/**
 * Un hilo consiguio el candado que estaba esperando.
 *
 * @since 1.3
 */
public interface MonitorContendedEnteredEvent extends LocatableEvent {

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
