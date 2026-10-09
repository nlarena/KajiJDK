package com.sun.jdi.event;

import com.sun.jdi.Mirror;

/**
 * Where the event sets are got from, one at a time.
 *
 * <p>{@link #remove()} blocks until there is something. A debugger has a thread dedicated to this
 * loop, because while it waits it can do nothing else.
 *
 * <p>When the connection is cut off, {@code remove} throws {@link VMDisconnectedException} after
 * delivering the {@link VMDisconnectEvent}: first the orderly notice, then the cut.
 *
 * @since 1.3
 */
public interface EventQueue extends Mirror {

    /**
     * The remove.
     *
     * @return the result
     * @throws InterruptedException if it applies
     */
    EventSet remove()
            throws InterruptedException;

    /**
     * The remove.
     *
     * @param index the long
     * @return the result
     * @throws InterruptedException if it applies
     */
    EventSet remove(long index)
            throws InterruptedException;
}
