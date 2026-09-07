package com.sun.jdi.event;

import java.util.Iterator;

/**
 * Recorre los eventos de un {@link EventSet} sin tener que convertirlos.
 *
 * @since 1.3
 */
public interface EventIterator extends Iterator<Event> {

    /**
     * El next event.
     *
     * @return el resultado
     */
    Event nextEvent();
}
