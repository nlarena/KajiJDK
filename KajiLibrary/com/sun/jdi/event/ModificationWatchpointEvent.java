package com.sun.jdi.event;

import com.sun.jdi.Value;

/**
 * A watched field was written.
 *
 * <p>It arrives <strong>before</strong> the writing happens: {@code valueCurrent} has the old one
 * and {@link #valueToBe} the new one.
 *
 * @since 1.3
 */
public interface ModificationWatchpointEvent extends WatchpointEvent {

    /**
     * The value to be.
     *
     * @return the result
     */
    Value valueToBe();
}
