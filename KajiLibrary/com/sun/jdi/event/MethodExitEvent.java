package com.sun.jdi.event;

import com.sun.jdi.Method;
import com.sun.jdi.Value;

/**
 * A method was left.
 *
 * <p>{@link #returnValue} gives what it returned, which is the only thing that allows a call's
 * result to be seen without modifying the program.
 *
 * @since 1.3
 */
public interface MethodExitEvent extends LocatableEvent {

    /**
     * The method.
     *
     * @return the result
     */
    Method method();

    /**
     * The return value.
     *
     * @return the result
     */
    Value returnValue();
}
