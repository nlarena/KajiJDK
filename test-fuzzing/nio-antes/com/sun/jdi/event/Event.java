package com.sun.jdi.event;

import com.sun.jdi.Mirror;
import com.sun.jdi.request.EventRequest;

/**
 * Something that happened in the debugged machine and that somebody asked to be told about.
 *
 * <p>An event does not arrive alone: it arrives inside an {@link EventSet}. And it does not
 * arrive just because: it arrives because there is an {@code EventRequest} that asked for it --
 * {@link #request} returns which.
 *
 * <p>That relation is the whole model of JDI: one asks, and afterwards it arrives.
 *
 * @since 1.3
 */
public interface Event extends Mirror {

    /**
     * The request.
     *
     * @return the result
     */
    EventRequest request();
}
