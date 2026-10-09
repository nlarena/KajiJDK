package com.sun.jdi.event;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;

/**
 * A class finished preparing itself in the debugged machine.
 *
 * <p>It is the moment when breakpoints may be put on it: before preparing it there is no code to
 * point at. A debugger that wants to break in a class that has not been loaded yet asks for this
 * event and waits.
 *
 * @since 1.3
 */
public interface ClassPrepareEvent extends Event {

    /**
     * The thread.
     *
     * @return the result
     */
    ThreadReference thread();

    /**
     * The reference type.
     *
     * @return the result
     */
    ReferenceType referenceType();
}
