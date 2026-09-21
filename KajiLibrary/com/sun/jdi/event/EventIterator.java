package com.sun.jdi.event;

import java.util.Iterator;

/**
 * It walks an {@link EventSet}'s events without having to convert them.
 *
 * @since 1.3
 */
public interface EventIterator extends Iterator<Event> {

    /**
     * The next event.
     *
     * @return the result
     */
    Event nextEvent();
}
