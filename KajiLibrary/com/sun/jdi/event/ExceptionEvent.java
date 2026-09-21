package com.sun.jdi.event;

import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;

/**
 * An exception was thrown in the debugged machine.
 *
 * <p>{@link #catchLocation} returns where it is going to be caught, or {@code null} if nobody
 * catches it. That distinction is what separates "break on any exception" from "break only on
 * those that are going to kill the program", which is what is almost always wanted.
 *
 * @since 1.3
 */
public interface ExceptionEvent extends LocatableEvent {

    /**
     * The exception.
     *
     * @return the result
     */
    ObjectReference exception();

    /**
     * The catch location.
     *
     * @return the result
     */
    Location catchLocation();
}
