package com.sun.jdi.event;

import com.sun.jdi.Method;

/**
 * Se entro a un metodo.
 *
 * @since 1.3
 */
public interface MethodEntryEvent extends LocatableEvent {

    /**
     * El method.
     *
     * @return el resultado
     */
    Method method();
}
