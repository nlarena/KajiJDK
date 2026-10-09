package com.sun.jdi.event;

import com.sun.jdi.Method;

/**
 * A method was entered.
 *
 * @since 1.3
 */
public interface MethodEntryEvent extends LocatableEvent {

    /**
     * The method.
     *
     * @return the result
     */
    Method method();
}
