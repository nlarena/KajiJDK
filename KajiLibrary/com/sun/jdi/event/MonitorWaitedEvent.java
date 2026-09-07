package com.sun.jdi.event;

import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;

/**
 * Un hilo salio de un {@code wait}.
 *
 * @since 1.3
 */
public interface MonitorWaitedEvent extends LocatableEvent {

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

    /**
     * El timedout.
     *
     * @return el resultado
     */
    boolean timedout();
}
